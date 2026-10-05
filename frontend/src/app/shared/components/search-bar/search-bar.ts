import { ChangeDetectionStrategy, Component, input, model } from '@angular/core';

@Component({
  selector: 'app-search-bar',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'search-bar' },
  template: `
    <label class="search-bar__field">
      <span class="sr-only">{{ placeholder() }}</span>
      <input
        type="search"
        class="search-bar__input"
        [placeholder]="placeholder()"
        [value]="value()"
        (input)="value.set($any($event.target).value)"
      />
    </label>
    @if (shortcut()) {
      <span class="search-bar__shortcut">{{ shortcut() }}</span>
    }
  `,
  styles: `
    :host {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 7px 12px;
      background: var(--color-surface-muted);
      border: 1px solid var(--color-border);
      border-radius: var(--radius-control);
      min-width: 220px;
    }
    :host(:focus-within) {
      border-color: var(--color-action);
      box-shadow: var(--focus-ring);
    }
    .search-bar__field {
      flex: 1;
      display: flex;
    }
    .search-bar__input {
      flex: 1;
      border: none;
      background: transparent;
      font-family: var(--font-ui);
      font-size: 13px;
      color: var(--color-ink);
      outline: none;
    }
    .search-bar__input::placeholder {
      color: var(--color-text-tertiary);
    }
    .search-bar__shortcut {
      font-family: var(--font-mono);
      font-size: 10.5px;
      padding: 2px 6px;
      border-radius: 4px;
      background: var(--color-surface);
      border: 1px solid var(--color-border);
      color: var(--color-text-tertiary);
    }
    .sr-only {
      position: absolute;
      width: 1px;
      height: 1px;
      overflow: hidden;
      clip: rect(0 0 0 0);
    }
  `,
})
export class SearchBar {
  readonly placeholder = input<string>('Rechercher…');
  readonly shortcut = input<string | undefined>(undefined);
  readonly value = model<string>('');
}
