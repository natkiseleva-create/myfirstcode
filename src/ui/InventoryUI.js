import { getBlockType } from '../blocks/blockTypes.js';
import { CraftingController } from '../inventory/CraftingController.js';

export class InventoryUI {
  /**
   * @param {import('../inventory/Inventory.js').Inventory} inventory
   * @param {{ onClose?: () => void }} [options]
   */
  constructor(inventory, options = {}) {
    this.inventory = inventory;
    this.onClose = options.onClose ?? (() => {});
    this.isOpen = false;
    this.crafting = new CraftingController(inventory);

    this.hotbarEl = document.getElementById('hotbar');
    this.backdropEl = document.getElementById('inventory-backdrop');
    this.panelEl = document.getElementById('inventory-panel');
    this.storageEl = document.getElementById('inventory-storage');
    this.panelHotbarEl = document.getElementById('inventory-hotbar');
    this.craftGridEl = document.getElementById('craft-grid');
    this.craftOutputEl = document.getElementById('craft-output');
    this.craftTab2 = document.getElementById('craft-tab-2');
    this.craftTab3 = document.getElementById('craft-tab-3');
    this.closeBtn = document.getElementById('btn-close-inventory');
    this.closeBottomBtn = document.getElementById('btn-close-inventory-bottom');
    this.crosshairEl = document.getElementById('crosshair');

    inventory.onChange(() => this.render());
    this.crafting.onChange(() => this.render());

    const closeHandler = (e) => {
      e.preventDefault();
      e.stopPropagation();
      this.close();
    };

    this.closeBtn?.addEventListener('click', closeHandler);
    this.closeBottomBtn?.addEventListener('click', closeHandler);

    this.backdropEl?.addEventListener('mousedown', (e) => {
      if (e.target === this.backdropEl) this.close();
    });

    this.panelEl?.addEventListener('mousedown', (e) => e.stopPropagation());

    this.craftTab2?.addEventListener('click', (e) => {
      e.stopPropagation();
      this.crafting.setGridSize(2);
      this._updateCraftTabs();
    });

    this.craftTab3?.addEventListener('click', (e) => {
      e.stopPropagation();
      this.crafting.setGridSize(3);
      this._updateCraftTabs();
    });

    this.craftOutputEl?.addEventListener('click', (e) => {
      e.stopPropagation();
      this.crafting.craftOutput();
    });

    this.render();
  }

  setOpen(open) {
    this.isOpen = open;

    this.hotbarEl?.classList.toggle('hidden', open);

    if (this.backdropEl) {
      this.backdropEl.classList.toggle('hidden', !open);
      this.backdropEl.setAttribute('aria-hidden', open ? 'false' : 'true');
    }

    if (this.crosshairEl) {
      this.crosshairEl.style.display = open ? 'none' : '';
    }

    if (!open) {
      this.crafting.clearGrid();
    }

    this.render();
  }

  open() {
    this.setOpen(true);
  }

  close() {
    if (!this.isOpen) return;
    this.setOpen(false);
    this.onClose();
  }

  toggle() {
    if (this.isOpen) {
      this.close();
      return false;
    }
    this.open();
    return true;
  }

  _updateCraftTabs() {
    const size = this.crafting.gridSize;
    this.craftTab2?.classList.toggle('active', size === 2);
    this.craftTab3?.classList.toggle('active', size === 3);
    if (this.craftGridEl) {
      this.craftGridEl.dataset.size = String(size);
    }
  }

  _renderCraftGrid() {
    if (!this.craftGridEl) return;
    this.craftGridEl.innerHTML = '';
    this._updateCraftTabs();

    const count = this.crafting.getSlotCount();
    for (let i = 0; i < count; i++) {
      const slot = this.crafting.getSlot(i);
      const el = document.createElement('button');
      el.type = 'button';
      el.className = 'inv-slot craft-slot';
      el.dataset.craftIndex = String(i);

      if (slot?.type) {
        this._fillSlotPreview(el, slot.type, slot.count);
      }

      el.addEventListener('mousedown', (e) => {
        e.stopPropagation();
        e.preventDefault();
        if (e.button === 2) {
          this.crafting.withdrawToInventory(i);
        } else {
          this.crafting.depositFromSelected(i);
        }
      });

      el.addEventListener('contextmenu', (e) => e.preventDefault());

      this.craftGridEl.appendChild(el);
    }

    this._renderOutputSlot();
  }

  _renderOutputSlot() {
    if (!this.craftOutputEl) return;
    this.craftOutputEl.innerHTML = '';
    const preview = this.crafting.getOutputPreview();

    if (preview?.type) {
      this._fillSlotPreview(this.craftOutputEl, preview.type, preview.count);
      this.craftOutputEl.classList.add('has-result');
      this.craftOutputEl.disabled = false;
    } else {
      this.craftOutputEl.classList.remove('has-result');
      this.craftOutputEl.disabled = true;
    }
  }

  _fillSlotPreview(el, typeId, count) {
    const blockType = getBlockType(typeId);
    const preview = document.createElement('span');
    preview.className = 'block-preview';
    if (blockType) {
      preview.style.setProperty('--top', colorToCss(blockType.topColor));
      preview.style.setProperty('--side', colorToCss(blockType.sideColor));
    }
    el.appendChild(preview);

    if (count > 1) {
      const countEl = document.createElement('span');
      countEl.className = 'slot-count';
      countEl.textContent = String(count);
      el.appendChild(countEl);
    }
  }

  render() {
    this._renderCraftGrid();
    this._renderBar(this.hotbarEl, 0, this.inventory.hotbarSize, true);
    if (this.storageEl) {
      this._renderBar(
        this.storageEl,
        this.inventory.hotbarSize,
        this.inventory.storageSize,
        false
      );
    }
    if (this.panelHotbarEl) {
      this._renderBar(this.panelHotbarEl, 0, this.inventory.hotbarSize, true);
    }
  }

  _renderBar(container, startIndex, count, isHotbar) {
    if (!container) return;
    container.innerHTML = '';

    for (let i = 0; i < count; i++) {
      const index = startIndex + i;
      const slot = this.inventory.getSlot(index);
      const el = document.createElement('button');
      el.type = 'button';
      el.className = 'inv-slot';
      el.dataset.slot = String(index);

      if (isHotbar && index === this.inventory.selectedSlot) {
        el.classList.add('selected');
      }

      if (isHotbar && index < 9) {
        const hint = document.createElement('span');
        hint.className = 'slot-key';
        hint.textContent = String(index + 1);
        el.appendChild(hint);
      }

      if (slot.type) {
        this._fillSlotPreview(el, slot.type, slot.count);
      }

      el.addEventListener('mousedown', (e) => {
        e.stopPropagation();
        if (index < this.inventory.hotbarSize) {
          this.inventory.selectSlot(index);
        }
      });

      container.appendChild(el);
    }
  }
}

function colorToCss(hex) {
  return `#${hex.toString(16).padStart(6, '0')}`;
}
