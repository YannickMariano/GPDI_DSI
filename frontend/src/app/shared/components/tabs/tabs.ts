import { ChangeDetectionStrategy, Component, input, model } from '@angular/core';

export interface TabItem {
  value: string;
  label: string;
  badge?: string | number;
}

export type TabsVariant = 'underline' | 'segmented' | 'pills';

@Component({
  selector: 'app-tabs',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'app-tabs', '[class]': '"app-tabs--" + variant()', role: 'tablist' },
  template: `
    @for (tab of items(); track tab.value) {
      <button
        type="button"
        role="tab"
        class="app-tabs__tab"
        [class.app-tabs__tab--active]="tab.value === selected()"
        [attr.aria-selected]="tab.value === selected()"
        (click)="selected.set(tab.value)"
      >
        {{ tab.label }}
        @if (tab.badge !== undefined) {
          <span class="app-tabs__badge">{{ tab.badge }}</span>
        }
      </button>
    }
  `,
  styles: `
    :host {
      display: flex;
      align-items: center;
      gap: 2px;
      overflow-x: auto;
    }
    .app-tabs__tab {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      border: none;
      background: transparent;
      font-family: var(--font-ui);
      font-size: 13px;
      font-weight: 500;
      color: var(--color-text-tertiary);
      cursor: pointer;
      white-space: nowrap;
    }
    :host(.app-tabs--underline) {
      border-bottom: 1px solid var(--color-border);
    }
    :host(.app-tabs--underline) .app-tabs__tab {
      padding: 10px 4px;
      margin-right: 20px;
      border-bottom: 2px solid transparent;
    }
    :host(.app-tabs--underline) .app-tabs__tab--active {
      color: var(--color-ink);
      font-weight: 700;
      border-bottom-color: var(--color-action);
    }
    :host(.app-tabs--segmented) {
      padding: 2px;
      background: var(--color-surface-alt);
      border-radius: var(--radius-control);
    }
    :host(.app-tabs--segmented) .app-tabs__tab {
      padding: 5px 12px;
      border-radius: 5px;
    }
    :host(.app-tabs--segmented) .app-tabs__tab--active {
      background: var(--color-surface);
      color: var(--color-ink);
      font-weight: 600;
      box-shadow: 0 1px 2px rgba(15, 23, 41, 0.06);
    }
    :host(.app-tabs--pills) .app-tabs__tab {
      padding: 6px 12px;
      border-radius: var(--radius-control);
      border: 1px solid var(--color-border-strong);
    }
    :host(.app-tabs--pills) .app-tabs__tab--active {
      border-color: var(--color-action);
      background: var(--color-action-bg);
      color: var(--color-action);
      font-weight: 600;
    }
    .app-tabs__badge {
      font-family: var(--font-mono);
      font-size: 10.5px;
      color: var(--color-text-quaternary);
    }
  `,
})
export class Tabs {
  readonly items = input.required<TabItem[]>();
  readonly variant = input<TabsVariant>('underline');
  readonly selected = model.required<string>();
}
