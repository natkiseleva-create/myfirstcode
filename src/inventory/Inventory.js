import { getBlockType } from '../blocks/blockTypes.js';

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
}
