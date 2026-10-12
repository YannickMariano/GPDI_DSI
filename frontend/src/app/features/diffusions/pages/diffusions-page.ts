import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { RapportDiffuse, RapportsDiffusesService } from '@core/services/rapports-diffuses.service';
import { AuthService } from '@core/services/auth.service';
import { Role } from '@core/models/role.model';
import { EmptyState } from '@shared/components/empty-state/empty-state';
import { ReportDocumentView } from '@shared/components/report-document/report-document';
import { formatDateFr } from '@shared/utils/format.util';
import { exporterRapportPdf } from '@shared/utils/export-pdf.util';
import { ToastService } from '@shared/components/toast/toast.service';

/**
 * Rapports diffusés par les chefs de projet, en lecture seule (consultation et export PDF) : ceux des
 * projets dont l'utilisateur est membre concerné (membre d'équipe), tous les projets pour le Directeur DSI.
 */
@Component({
  selector: 'app-diffusions-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [EmptyState, ReportDocumentView],
  templateUrl: './diffusions-page.html',
  styleUrl: './diffusions-page.css',
})
export class DiffusionsPage {
  private readonly rapportsDiffusesService = inject(RapportsDiffusesService);
  private readonly authService = inject(AuthService);
  private readonly toast = inject(ToastService);

  protected readonly formatDateFr = formatDateFr;
  protected readonly charge = this.rapportsDiffusesService.charge;
  protected readonly estDirecteur = computed(() => this.authService.currentUser()?.role === Role.DirecteurDsi);

  /** Projets ayant au moins un rapport diffusé, pour le filtre. */
  protected readonly projets = computed(() => {
    const parId = new Map<string, string>();
    for (const r of this.rapportsDiffusesService.rapports()) parId.set(r.projectId, r.nomProjet);
    return [...parId].map(([id, nom]) => ({ id, nom })).sort((a, b) => a.nom.localeCompare(b.nom));
  });

  protected readonly projetFiltre = signal('');
  protected readonly rapports = computed(() => {
    const filtre = this.projetFiltre();
    return this.rapportsDiffusesService.rapports().filter((r) => !filtre || r.projectId === filtre);
  });

  private readonly rapportChoisi = signal<string | undefined>(undefined);
  protected readonly rapportSelectionne = computed<RapportDiffuse | undefined>(() => {
    const liste = this.rapports();
    return liste.find((r) => r.id === this.rapportChoisi()) ?? liste.at(0);
  });

  constructor() {
    // Recharge à chaque ouverture : de nouveaux rapports ont pu être diffusés depuis la dernière visite.
    void this.rapportsDiffusesService.reload();
  }

  selectRapport(id: string): void {
    this.rapportChoisi.set(id);
  }

  protected readonly exportEnCours = signal(false);

  /** Télécharge le rapport diffusé affiché au format PDF. */
  async exporterPdf(): Promise<void> {
    const rapport = this.rapportSelectionne();
    if (!rapport?.document || this.exportEnCours()) return;
    this.exportEnCours.set(true);
    try {
      await exporterRapportPdf(`${rapport.nom} — ${rapport.nomProjet}`);
    } catch {
      this.toast.show({ type: 'error', message: "L'export PDF a échoué. Veuillez réessayer." });
    } finally {
      this.exportEnCours.set(false);
    }
  }
}
