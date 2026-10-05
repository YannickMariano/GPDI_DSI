import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'app-referentiels-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="referentiels">
      <h1 class="referentiels__title">Référentiels</h1>
      <section class="referentiels__panel">
        <p class="referentiels__placeholder">Gestion des référentiels (directions, natures de livrables…) — à venir.</p>
      </section>
    </div>
  `,
  styles: `
    .referentiels {
      display: flex;
      flex-direction: column;
      gap: 16px;
      padding: 24px;
    }
    .referentiels__title {
      margin: 0;
      font-size: 22px;
      font-weight: 800;
      letter-spacing: -0.02em;
    }
    .referentiels__panel {
      background: var(--color-surface);
      border: 1px solid var(--color-border);
      border-radius: var(--radius-card);
      padding: 18px 20px;
    }
    .referentiels__placeholder {
      margin: 0;
      font-size: 13px;
      color: var(--color-text-tertiary);
    }
  `,
})
export class ReferentielsPage {}
