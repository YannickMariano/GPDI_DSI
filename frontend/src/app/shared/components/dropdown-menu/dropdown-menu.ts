import { ChangeDetectionStrategy, Component, input, output, signal } from '@angular/core';
import { ClickOutside } from '@shared/directives/click-outside';

export interface DropdownItem {
  value: string;
  label: string;
}

@Component({
  selector: 'app-dropdown-menu',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ClickOutside],
  host: {
    class: 'dropdown',
    '(keydown.escape)': 'isOpen.set(false)',
  },
  template: `
    <div appClickOutside (appClickOutside)="isOpen.set(false)">
      <button type="button" class="dropdown__trigger" [attr.aria-expanded]="isOpen()" (click)="isOpen.set(!isOpen())">
        {{ label() }} <span aria-hidden="true">▾</span>
      </button>
      @if (isOpen()) {
        <ul class="dropdown__menu" role="menu">
          @for (item of items(); track item.value) {
            <li role="none">
              <button type="button" role="menuitem" class="dropdown__item" (click)="onSelect(item)">{{ item.label }}</button>
            </li>
          }
        </ul>
      }
    </div>
  `,
  styles: `
    :host {
      position: relative;
      display: inline-block;
    }
    .dropdown__trigger {
      display: inline-flex;
      align-items: center;
      gap: 7px;
      padding: 6px 11px;
      border: 1px solid var(--color-border-strong);
      border-radius: var(--radius-control);
      background: var(--color-surface);
      font-size: 12.5px;
      font-weight: 500;
      color: var(--color-text-secondary);
      cursor: pointer;
    }
    .dropdown__trigger:hover {
      background: var(--color-surface-muted);
    }
    .dropdown__menu {
      position: absolute;
      top: calc(100% + 6px);
      left: 0;
      min-width: 200px;
      margin: 0;
      padding: 6px;
      list-style: none;
      background: var(--color-surface);
      border: 1px solid var(--color-border);
      border-radius: var(--radius-control);
      box-shadow: var(--shadow-elevated);
      z-index: 20;
    }
    .dropdown__item {
      display: block;
      width: 100%;
      text-align: left;
      padding: 8px 10px;
      border: none;
      background: transparent;
      border-radius: 5px;
      font-size: 13px;
      color: var(--color-ink);
      cursor: pointer;
    }
    .dropdown__item:hover {
      background: var(--color-surface-alt);
    }
  `,
})
export class DropdownMenu {
  readonly label = input.required<string>();
  readonly items = input.required<DropdownItem[]>();
  readonly select = output<DropdownItem>();
  protected readonly isOpen = signal(false);

  protected onSelect(item: DropdownItem): void {
    this.select.emit(item);
    this.isOpen.set(false);
  }
}
