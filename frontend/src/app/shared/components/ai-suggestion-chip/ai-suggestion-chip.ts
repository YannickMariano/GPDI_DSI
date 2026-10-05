import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'app-ai-suggestion-chip',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'ai-chip' },
  template: `
    <span aria-hidden="true">✦</span>
    <span><ng-content /></span>
  `,
  styles: `
    :host {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 3px 9px;
      border-radius: 4px;
      background: var(--color-ai-bg);
      color: var(--color-ai);
      font-size: 11.5px;
      font-weight: 600;
      width: fit-content;
    }
  `,
})
export class AiSuggestionChip {
  readonly confiance = input<number | undefined>(undefined);
}
