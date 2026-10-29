import { ChangeDetectionStrategy, Component, ElementRef, computed, inject, signal, viewChild } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink } from '@angular/router';
import { filter, map } from 'rxjs';
import { AuthService } from '@core/services/auth.service';
import { AssistantIaService } from '@core/services/assistant-ia.service';
import { Role } from '@core/models/role.model';
import { AssistantConversation } from '@features/ai-assistant/components/assistant-conversation/assistant-conversation';

const ROUTE_ASSISTANT_IA = '/assistant-ia';

/**
 * Bulle de discussion avec l'assistant IA, disponible sur toutes les pages (hors onglet Assistant IA, qui affiche
 * déjà le même fil). Panneau non bloquant : la navigation reste possible pendant que l'IA répond.
 */
@Component({
  selector: 'app-assistant-widget',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AssistantConversation, RouterLink],
  host: { '(document:keydown.escape)': 'fermer()' },
  templateUrl: './assistant-widget.html',
  styleUrl: './assistant-widget.css',
})
export class AssistantWidget {
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly assistantIaService = inject(AssistantIaService);

  private readonly url = toSignal(
    this.router.events.pipe(
      filter((event) => event instanceof NavigationEnd),
      map(() => this.router.url),
    ),
    { initialValue: this.router.url },
  );

  /** Même public que l'onglet Assistant IA (route gardée par roleGuard(Role.ChefProjet)). */
  protected readonly visible = computed(
    () => this.authService.currentUser()?.role === Role.ChefProjet && !this.url().startsWith(ROUTE_ASSISTANT_IA),
  );
  protected readonly ouvert = signal(false);
  protected readonly enCours = this.assistantIaService.enCoursGlobal;

  private readonly bouton = viewChild<ElementRef<HTMLButtonElement>>('bouton');
  private readonly conversation = viewChild(AssistantConversation);

  basculer(): void {
    if (this.ouvert()) {
      this.fermer();
      return;
    }
    this.ouvert.set(true);
    setTimeout(() => this.conversation()?.focusSaisie());
  }

  fermer(): void {
    if (!this.ouvert()) return;
    this.ouvert.set(false);
    setTimeout(() => this.bouton()?.nativeElement.focus());
  }
}
