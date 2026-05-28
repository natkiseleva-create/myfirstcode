import { getBlockType } from '../blocks/blockTypes.js';
import { CRAFT_RECIPES } from '../inventory/crafting.js';

export class InventoryUI {
  /**
   * @param {import('../inventory/Inventory.js').Inventory} inventory
   * @param {{ onClose?: () => void }} [options]
   */
  constructor(inventory, options = {}) {
    this.inventory = inventory;
    this.onClose = options.onClose ?? (() => {});
    this.isOpen = false;

    this.hotbarEl = document.getElementById('hotbar');
    this.backdropEl = document.getElementById('inventory-backdrop');
    this.panelEl = document.getElementById('inventory-panel');
    this.storageEl = document.getElementById('inventory-storage');
    this.panelHotbarEl = document.getElementById('inventory-hotbar');
    this.craftPlanksBtn = document.getElementById('craft-planks');
    this.closeBtn = document.getElementById('btn-close-inventory');

    inventory.onChange(() => this.render());

    this.craftPlanksBtn?.addEventListener('click', (e) => {
      e.stopPropagation();
      this.inventory.craft('wood_to_planks', this.inventory.selectedSlot);
    });

    this.closeBtn?.addEventListener('click', (e) => {
      e.stopPropagation();
      this.close();
    });

    this.backdropEl?.addEventListener('click', () => this.close());
    this.panelEl?.addEventListener('click', (e) => e.stopPropagation());

    this.render();
  }

  setOpen(open) {
    this.isOpen = open;
    this.hotbarEl?.classList.toggle('hidden', open);
    this.backdropEl?.classList.toggle('hidden', !open);
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

  _updateCraftButtons() {
    const recipe = CRAFT_RECIPES.wood_to_planks;
    if (!this.craftPlanksBtn || !recipe) return;

    const woodCount = this.inventory.countItem(recipe.input.type);
    const canCraft = woodCount >= recipe.input.count;

    this.craftPlanksBtn.disabled = !canCraft;
    this.craftPlanksBtn.title = canCraft
      ? `${recipe.input.count} дерево → ${recipe.output.count} доски`
      : 'Нужно дерево (брёвна)';
  }

  render() {
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
    this._updateCraftButtons();
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
        const blockType = getBlockType(slot.type);
        const preview = document.createElement('span');
        preview.className = 'block-preview';
        if (blockType) {
          preview.style.setProperty('--top', colorToCss(blockType.topColor));
          preview.style.setProperty('--side', colorToCss(blockType.sideColor));
        }
        el.appendChild(preview);

        if (slot.count > 1) {
          const count = document.createElement('span');
          count.className = 'slot-count';
          count.textContent = String(slot.count);
          el.appendChild(count);
        }
      }

      el.addEventListener('click', (e) => {
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
