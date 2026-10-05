import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

@Component({
  selector: 'app-progress-bar',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    class: 'progress-bar',
    '[style.height.px]': 'height()',
    role: 'progressbar',
    '[attr.aria-valuenow]': 'clampedValue()',
    '[attr.aria-label]': 'ariaLabel() ?? "Progression : " + clampedValue() + " %"',
    'aria-valuemin': '0',
    'aria-valuemax': '100',
  },
  template: `<span class="progress-bar__fill" [style.width.%]="clampedValue()" [style.background]="color()"></span>`,
  styles: `
    :host {
      display: block;
      width: 100%;
      border-radius: 3px;
      background: var(--color-border);
      overflow: hidden;
    }
    .progress-bar__fill {
      display: block;
      height: 100%;
      border-radius: inherit;
    }
  `,
})
export class ProgressBar {
  readonly value = input.required<number>();
  readonly color = input<string>('var(--color-action)');
  readonly height = input<number>(6);
  readonly ariaLabel = input<string | undefined>(undefined);
  protected readonly clampedValue = computed(() => Math.max(0, Math.min(100, this.value())));
}
