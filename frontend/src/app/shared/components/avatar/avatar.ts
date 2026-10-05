import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'app-avatar',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    class: 'avatar',
    '[style.width.px]': 'size()',
    '[style.height.px]': 'size()',
    '[style.background]': 'bg()',
    '[style.fontSize.px]': 'size() > 24 ? 11 : 9',
    '[attr.title]': 'nom() ?? null',
    role: 'img',
    '[attr.aria-label]': 'nom() ?? initiales()',
  },
  template: `{{ initiales() }}`,
  styles: `
    :host {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      border-radius: 50%;
      color: #ffffff;
      font-weight: 700;
      flex: 0 0 auto;
      font-family: var(--font-ui);
    }
  `,
})
export class Avatar {
  readonly initiales = input.required<string>();
  readonly bg = input<string>('#475467');
  readonly size = input<number>(28);
  readonly nom = input<string | undefined>(undefined);
}
