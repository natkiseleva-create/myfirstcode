import { matchRecipe } from './recipes.js';
import { getBlockType } from '../blocks/blockTypes.js';

/**
 * Minecraft-style craft grid (2x2 or 3x3) with output slot.
 */
export class CraftingController {
  /**
   * @param {import('./Inventory.js').Inventory} inventory
   */
  constructor(inventory) {
    this.inventory = inventory;
    this.gridSize = 2;
    /** @type {({ type: string, count: number } | null)[]} */
    this.slots = Array(9).fill(null);
    this.listeners = new Set();
  }

  onChange(cb) {
    this.listeners.add(cb);
    return () => this.listeners.delete(cb);
  }

  _notify() {
    for (const cb of this.listeners) cb(this);
  }

  setGridSize(size) {
    if (size !== 2 && size !== 3) return;
    if (this.gridSize === size) return;
    this.gridSize = size;
    const max = size * size;
    for (let i = max; i < 9; i++) {
      this._returnSlotToInventory(i);
    }
    this._notify();
  }

  getSlotCount() {
    return this.gridSize * this.gridSize;
  }

  getSlot(index) {
    return this.slots[index] ?? null;
  }

  /** @returns {(string|null)[][]} */
  getCellsMatrix() {
    const size = this.gridSize;
    const rows = [];
    for (let y = 0; y < size; y++) {
      const row = [];
      for (let x = 0; x < size; x++) {
        const slot = this.slots[y * size + x];
        row.push(slot?.type ?? null);
      }
      rows.push(row);
    }
    return rows;
  }

  getMatch() {
    return matchRecipe(this.getCellsMatrix(), this.gridSize);
  }

  getOutputPreview() {
    return this.getMatch()?.output ?? null;
  }

  _returnSlotToInventory(index) {
    const slot = this.slots[index];
    if (!slot?.type) return;
    this.inventory.addItem(slot.type, slot.count);
    this.slots[index] = null;
  }

  clearGrid() {
    for (let i = 0; i < 9; i++) {
      this._returnSlotToInventory(i);
    }
    this._notify();
  }

  /**
   * Put one item from selected hotbar slot into craft slot.
   * @param {number} index
   */
  depositFromSelected(index) {
    if (index >= this.getSlotCount()) return false;

    const hotbar = this.inventory.getSelectedItem();
    if (!hotbar?.type || hotbar.count <= 0) return false;

    const craft = this.slots[index];
    if (!craft) {
      if (!this.inventory.consumeSelected(1)) return false;
      this.slots[index] = { type: hotbar.type, count: 1 };
      this._notify();
      return true;
    }

    if (craft.type !== hotbar.type) return false;
    const max = getBlockType(craft.type)?.stackSize ?? 64;
    if (craft.count >= max) return false;
    if (!this.inventory.consumeSelected(1)) return false;
    craft.count += 1;
    this._notify();
    return true;
  }

  /**
   * Return one item from craft slot to inventory.
   * @param {number} index
   */
  withdrawToInventory(index) {
    const craft = this.slots[index];
    if (!craft?.type) return false;

    const added = this.inventory.addItem(craft.type, 1);
    if (added <= 0) return false;

    craft.count -= 1;
    if (craft.count <= 0) this.slots[index] = null;
    this._notify();
    return true;
  }

  /** Craft result into player inventory. */
  craftOutput() {
    const match = this.getMatch();
    if (!match) return false;

    const added = this.inventory.addItem(match.output.type, match.output.count);
    if (added < match.output.count) return false;

    const size = this.getSlotCount();
    for (let i = 0; i < size; i++) {
      const slot = this.slots[i];
      if (!slot) continue;
      slot.count -= 1;
      if (slot.count <= 0) this.slots[i] = null;
    }

    this._notify();
    return true;
  }
}
