import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-access-denied',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink],
  template: `
    <div class="access-denied">
      <div class="access-denied__icon" aria-hidden="true">⛔</div>
      <h1>Accès refusé</h1>
      <p>Votre rôle ne vous permet pas d'accéder à cet écran.</p>
      <a routerLink="/">Retour à l'accueil</a>
    </div>
  `,
  styles: `
    .access-denied {
      min-height: 100dvh;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 10px;
      text-align: center;
      padding: 24px;
    }
    .access-denied__icon {
      font-size: 32px;
    }
    h1 {
      margin: 0;
      font-size: 22px;
    }
    p {
      margin: 0;
      color: var(--color-text-secondary);
    }
  `,
})
export class AccessDenied {}
