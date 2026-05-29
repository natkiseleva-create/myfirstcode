import { getBlockType } from '../blocks/blockTypes.js';
import { CRAFT_RECIPES } from './crafting.js';

const HOTBAR_SIZE = 9;
const STORAGE_SIZE = 27;

export class Inventory {
  constructor() {
    this.hotbarSize = HOTBAR_SIZE;
    this.storageSize = STORAGE_SIZE;
    this.slots = Array(HOTBAR_SIZE + STORAGE_SIZE).fill(null).map(() => ({
      type: null,
      count: 0,
    }));
    this.selectedSlot = 0;
    this.listeners = new Set();
  }

  onChange(listener) {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  }

  _notify() {
    for (const listener of this.listeners) {
      listener(this);
    }
  }

  getSlot(index) {
    return this.slots[index] ?? null;
  }

  getSelectedItem() {
    return this.slots[this.selectedSlot];
  }

  selectSlot(index) {
    if (index < 0 || index >= this.hotbarSize) return;
    this.selectedSlot = index;
    this._notify();
  }

  cycleSelection(delta) {
    const next =
      (this.selectedSlot + delta + this.hotbarSize) % this.hotbarSize;
    this.selectSlot(next);
  }

  addItem(typeId, amount = 1) {
    const blockType = getBlockType(typeId);
    if (!blockType || amount <= 0) return 0;

    let remaining = amount;

    for (const slot of this.slots) {
      if (remaining <= 0) break;
      if (slot.type === typeId && slot.count < blockType.stackSize) {
        const space = blockType.stackSize - slot.count;
        const added = Math.min(space, remaining);
        slot.count += added;
        remaining -= added;
      }
    }

    for (const slot of this.slots) {
      if (remaining <= 0) break;
      if (slot.type === null) {
        const added = Math.min(blockType.stackSize, remaining);
        slot.type = typeId;
        slot.count = added;
        remaining -= added;
      }
    }

    const added = amount - remaining;
    if (added > 0) this._notify();
    return added;
  }

  _removeFromSlot(slot, amount) {
    if (!slot.type || slot.count < amount) return 0;
    const removed = Math.min(slot.count, amount);
    slot.count -= removed;
    if (slot.count <= 0) {
      slot.type = null;
      slot.count = 0;
    }
    return removed;
  }

  /**
   * @param {string} typeId
   * @param {number} amount
   * @param {number | null} [preferredSlot]
   */
  removeItems(typeId, amount, preferredSlot = null) {
    let remaining = amount;

    if (
      preferredSlot !== null &&
      preferredSlot >= 0 &&
      preferredSlot < this.slots.length
    ) {
      const slot = this.slots[preferredSlot];
      if (slot.type === typeId) {
        remaining -= this._removeFromSlot(slot, remaining);
      }
    }

    if (remaining <= 0) {
      this._notify();
      return true;
    }

    for (let i = 0; i < this.slots.length; i++) {
      if (remaining <= 0) break;
      if (i === preferredSlot) continue;
      const slot = this.slots[i];
      if (slot.type === typeId) {
        remaining -= this._removeFromSlot(slot, remaining);
      }
    }

    if (remaining <= 0) {
      this._notify();
      return true;
    }

    return false;
  }

  /**
   * @param {string} recipeId
   * @param {number | null} [preferredSlot]
   */
  craft(recipeId, preferredSlot = null) {
    const recipe = CRAFT_RECIPES[recipeId];
    if (!recipe) return false;

    const slot =
      preferredSlot !== null ? preferredSlot : this.selectedSlot;

    if (!this.removeItems(recipe.input.type, recipe.input.count, slot)) {
      return false;
    }

    const added = this.addItem(recipe.output.type, recipe.output.count);
    if (added < recipe.output.count) {
      this.addItem(recipe.input.type, recipe.input.count);
      return false;
    }

    return true;
  }

  consumeSelected(amount = 1) {
    const slot = this.slots[this.selectedSlot];
    if (!slot.type || slot.count < amount) return false;

    slot.count -= amount;
    if (slot.count <= 0) {
      slot.type = null;
      slot.count = 0;
    }

    this._notify();
    return true;
  }

  hasSelected() {
    const slot = this.getSelectedItem();
    return Boolean(slot.type && slot.count > 0);
  }

  countItem(typeId) {
    let total = 0;
    for (const slot of this.slots) {
      if (slot.type === typeId) total += slot.count;
    }
    return total;
  }
}
