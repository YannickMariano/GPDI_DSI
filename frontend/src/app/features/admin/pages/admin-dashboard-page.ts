import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { PeopleService } from '@core/services/people.service';
import { EquipesService } from '@core/services/equipes.service';
import { PartagesClientService } from '@core/services/partages-client.service';
import { JournalConnexionService } from '@core/services/journal-connexion.service';
import { Role } from '@core/models/role.model';
import { KpiCard } from '@shared/components/kpi-card/kpi-card';

type Ton = 'succes' | 'alerte' | 'erreur' | 'neutre';

const TON_COLOR: Record<Ton, string> = {
  succes: 'var(--color-success-text)',
  alerte: 'var(--color-alert)',
  erreur: 'var(--color-error-text)',
  neutre: 'var(--color-text-tertiary)',
};

const RESULTAT_DOT: Record<string, string> = {
  succes: 'var(--color-success)',
  echec: 'var(--color-error)',
};

@Component({
  selector: 'app-admin-dashboard-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, KpiCard],
  templateUrl: './admin-dashboard-page.html',
  styleUrl: './admin-dashboard-page.css',
})
export class AdminDashboardPage {
  private readonly peopleService = inject(PeopleService);
  private readonly equipesService = inject(EquipesService);
  private readonly partagesClientService = inject(PartagesClientService);
  private readonly journalConnexionService = inject(JournalConnexionService);

  protected readonly tonColor = TON_COLOR;

  private readonly totalComptes = computed(() => this.peopleService.people().length);
  private readonly actifs = this.peopleService.activeCount;
  private readonly suspendus = this.peopleService.suspendedCount;
  private readonly totalRoles = Object.values(Role).length;
  private readonly teams = this.equipesService.teams;
  private readonly agentsRattaches = computed(() => new Set(this.teams().flatMap((t) => [t.chefId, ...t.membreIds])).size);
  private readonly shares = this.partagesClientService.shares;
  private readonly partagesActifs = computed(() => this.shares().filter((s) => s.etat === 'actif').length);
  private readonly partagesRevoques = computed(() => this.shares().filter((s) => s.etat !== 'actif').length);
  private readonly connectionLog = this.journalConnexionService.evenements;
  private readonly echecs = computed(() => this.connectionLog().filter((e) => e.resultat === 'echec').length);

  protected readonly kpis = computed(() => [
    {
      libelle: 'Comptes actifs',
      valeur: `${this.actifs()}`,
      detail: this.suspendus() > 0 ? `${this.suspendus()} suspendu(s)` : 'Aucun compte suspendu',
      ton: (this.suspendus() > 0 ? 'alerte' : 'succes') as Ton,
    },
    {
      libelle: 'Rôles définis',
      valeur: `${this.totalRoles}`,
      detail: 'Rôles système fixes',
      ton: 'neutre' as Ton,
    },
    {
      libelle: 'Équipes',
      valeur: `${this.teams().length}`,
      detail: `${this.agentsRattaches()} agent(s) rattaché(s)`,
      ton: 'neutre' as Ton,
    },
    {
      libelle: 'Partages client actifs',
      valeur: `${this.partagesActifs()}`,
      detail: this.partagesRevoques() > 0 ? `${this.partagesRevoques()} révoqué(s)` : 'Aucun partage révoqué',
      ton: (this.partagesRevoques() > 0 ? 'alerte' : 'succes') as Ton,
    },
  ]);

  protected readonly domaines = computed(() => [
    {
      icone: '▤',
      nom: 'Comptes utilisateurs',
      route: '/administration/comptes',
      chiffres: [
        { valeur: `${this.totalComptes()}`, libelle: 'comptes' },
        { valeur: `${this.actifs()}`, libelle: 'actifs' },
        { valeur: `${this.suspendus()}`, libelle: 'suspendus' },
      ],
      alerte: this.suspendus() > 0 ? `${this.suspendus()} compte(s) suspendu(s) à vérifier.` : undefined,
      action: 'Gérer les comptes',
    },
    {
      icone: '◒',
      nom: 'Rôles et habilitations',
      route: '/administration/roles',
      chiffres: [
        { valeur: `${this.totalRoles}`, libelle: 'rôles définis' },
      ],
      alerte: undefined as string | undefined,
      action: 'Gérer les rôles',
    },
    {
      icone: '◍',
      nom: 'Équipes de projet',
      route: '/administration/equipe-projet',
      chiffres: [
        { valeur: `${this.teams().length}`, libelle: 'équipes' },
        { valeur: `${this.agentsRattaches()}`, libelle: 'agents rattachés' },
      ],
      alerte: undefined as string | undefined,
      action: 'Gérer les équipes',
    },
    {
      icone: '◴',
      nom: "Journal d'accès",
      route: '/administration/journal-acces',
      chiffres: [
        { valeur: `${this.connectionLog().length}`, libelle: 'événements' },
        { valeur: `${this.echecs()}`, libelle: 'échecs' },
      ],
      alerte: this.echecs() > 0 ? `${this.echecs()} échec(s) de connexion récents.` : undefined,
      action: 'Consulter le journal',
    },
    {
      icone: '◇',
      nom: 'Partage client',
      route: '/administration/partage-client',
      chiffres: [
        { valeur: `${this.partagesActifs()}`, libelle: 'actifs' },
        { valeur: `${this.partagesRevoques()}`, libelle: 'révoqués' },
      ],
      alerte: undefined as string | undefined,
      action: 'Gérer les partages',
    },
  ]);

  protected readonly activiteRecente = computed(() =>
    this.connectionLog()
      .slice(0, 6)
      .map((e) => ({
        heure: e.horodatage.slice(11, 16),
        type: e.nomAffiche,
        detail: e.evenement,
        dot: RESULTAT_DOT[e.resultat],
      })),
  );

  protected readonly pointsVigilance = computed(() => {
    const points: { texte: string; source: string; ton: Ton }[] = [];

    if (this.suspendus() > 0) {
      points.push({ texte: `${this.suspendus()} compte(s) suspendu(s) : à vérifier avant toute réactivation.`, source: 'Comptes utilisateurs', ton: 'alerte' });
    }

    const echecsParMatricule = new Map<string, { count: number; nom: string }>();
    for (const e of this.connectionLog()) {
      if (e.resultat !== 'echec') continue;
      const actuel = echecsParMatricule.get(e.matricule);
      echecsParMatricule.set(e.matricule, { count: (actuel?.count ?? 0) + 1, nom: e.nomAffiche });
    }
    for (const { count, nom } of echecsParMatricule.values()) {
      if (count >= 2) {
        points.push({ texte: `${nom} a échoué ${count} fois à se connecter.`, source: "Journal d'accès", ton: 'erreur' });
      }
    }

    for (const share of this.shares().filter((s) => s.etat === 'revoque')) {
      const client = this.peopleService.byId(share.clientId)?.nom ?? 'un client';
      points.push({ texte: `Partage révoqué avec ${client} sur « ${share.titreProjet} ».`, source: 'Partage client', ton: 'neutre' });
    }

    return points.slice(0, 4);
  });
}
