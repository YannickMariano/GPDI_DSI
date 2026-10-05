import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'app-skeleton-loader',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    class: 'skeleton',
    '[style.height.px]': 'height()',
    '[style.borderRadius.px]': 'radius()',
    role: 'status',
    'aria-label': 'Chargement en cours',
  },
  template: ``,
  styles: `
    :host {
      display: block;
      width: 100%;
      background: linear-gradient(90deg, var(--color-border) 25%, var(--color-surface-alt) 37%, var(--color-border) 63%);
      background-size: 400% 100%;
      animation: skeleton-shimmer 1.4s ease infinite;
    }
    @keyframes skeleton-shimmer {
      0% { background-position: 100% 50%; }
      100% { background-position: 0 50%; }
    }
    @media (prefers-reduced-motion: reduce) {
      :host { animation: none; }
    }
  `,
})
export class SkeletonLoader {
  readonly height = input<number>(16);
  readonly radius = input<number>(6);
}
