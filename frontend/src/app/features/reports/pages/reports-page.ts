import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { ReportsService } from '@core/services/reports.service';
import { ProjectsService } from '@core/services/projects.service';
import { TasksService } from '@core/services/tasks.service';
import { RisquesProjetService } from '@core/services/risques-projet.service';
import { AuthService } from '@core/services/auth.service';
import { MilestonesService } from '@core/services/milestones.service';
import { SprintsService } from '@core/services/sprints.service';
import { PeopleService } from '@core/services/people.service';
import { RecommandationsProjetService } from '@core/services/recommandations-projet.service';
import { criticiteRisque, Criticite } from '@core/models/risk.model';
import { StatutTache, Task } from '@core/models/task.model';
import { ReportDocument, ReportKpi, Ton } from '@core/models/report.model';
import { ToastService } from '@shared/components/toast/toast.service';
import { formatDateFr, formatHours } from '@shared/utils/format.util';
import { exporterRapportPdf } from '@shared/utils/export-pdf.util';
import { ReportDocumentView } from '@shared/components/report-document/report-document';
import { RapportsDiffusesService } from '@core/services/rapports-diffuses.service';

const CRITICITE_LABEL: Record<Criticite, { label: string; ton: Ton }> = {
  critique: { label: 'Critique', ton: 'erreur' },
  eleve: { label: 'Élevé', ton: 'erreur' },
  modere: { label: 'Modéré', ton: 'alerte' },
  faible: { label: 'Faible', ton: 'neutre' },
};

const STATUT_TACHE_LABEL: Record<StatutTache, { label: string; ton: Ton }> = {
  a_faire: { label: 'À faire', ton: 'neutre' },
  en_cours: { label: 'En cours', ton: 'neutre' },
  en_test: { label: 'En test', ton: 'neutre' },
  bloquee: { label: 'Bloquée', ton: 'erreur' },
  terminee: { label: 'Terminé', ton: 'succes' },
  reportee: { label: 'Reportée', ton: 'alerte' },
};

const SANTE_LABEL: Record<string, { label: string; ton: Ton }> = {
  sain: { label: 'Sain', ton: 'succes' },
  sous_surveillance: { label: 'Sous surveillance', ton: 'alerte' },
  a_risque: { label: 'À risque', ton: 'erreur' },
};

const MEF = "Ministère de l'Économie et des Finances — DSI";
const kpi = (libelle: string, valeur: string, delta: string, ton: Ton): ReportKpi => ({ libelle, valeur, delta, ton });

/** Heures arrondies à l'heure entière, sans décimale, avec les jours au-delà de 24 h (ex. « 12 h », « 12 j 5 h »). */
const heures = (valeur: number): string => formatHours(Math.round(valeur));
/** Écart d'heures arrondi, avec son signe (ex. « +12 h », « -1 j 3 h »). */
const heuresSignees = (valeur: number): string => {
  const arrondi = Math.round(valeur);
  return `${arrondi >= 0 ? '+' : ''}${formatHours(arrondi)}`;
};

@Component({
  selector: 'app-reports-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './reports-page.html',
  imports: [ReportDocumentView],
  styleUrl: './reports-page.css',
})
export class ReportsPage {
  private readonly reportsService = inject(ReportsService);
  private readonly projectsService = inject(ProjectsService);
  private readonly tasksService = inject(TasksService);
  private readonly risquesService = inject(RisquesProjetService);
  private readonly authService = inject(AuthService);
  private readonly milestonesService = inject(MilestonesService);
  private readonly sprintsService = inject(SprintsService);
  private readonly peopleService = inject(PeopleService);
  private readonly recommandationsService = inject(RecommandationsProjetService);
  private readonly toast = inject(ToastService);
  private readonly rapportsDiffusesService = inject(RapportsDiffusesService);

  protected readonly formatDateFr = formatDateFr;
  protected readonly templates = this.reportsService.templates;
  protected readonly selectedId = signal(this.templates()[0]?.id ?? '');
  protected readonly selectedTemplate = computed(() => this.templates().find((t) => t.id === this.selectedId()));

  /** Projets sur lesquels un rapport peut être produit : ceux du chef de projet, tous pour le Directeur DSI. */
  protected readonly projetsDisponibles = computed(() => {
    const user = this.authService.currentUser();
    const projets = this.projectsService.projects();
    return user ? this.projectsService.pourUtilisateur(user, []) : projets;
  });
  private readonly projetChoisi = signal<string | undefined>(undefined);
  protected readonly projetId = computed(() => {
    const choisi = this.projetChoisi();
    const dispo = this.projetsDisponibles();
    return choisi && dispo.some((p) => p.id === choisi) ? choisi : (dispo[0]?.id ?? '');
  });

  private readonly projet = computed(() => this.projectsService.projects().find((p) => p.id === this.projetId()));
  private readonly taches = computed(() => this.tasksService.tasks().filter((t) => t.projectId === this.projetId()));
  private readonly risquesTous = computed(() => this.risquesService.risques().filter((r) => r.projectId === this.projetId()));
  private readonly risquesActifs = computed(() => this.risquesTous().filter((r) => r.statut !== 'clos'));
  private readonly jalons = computed(() => this.milestonesService.milestones().filter((m) => m.projectId === this.projetId()));
  private readonly sprints = computed(() => this.sprintsService.sprints().filter((s) => s.projectId === this.projetId()));

  constructor() {
    effect(() => {
      const id = this.projetId();
      if (!id) return;
      this.tasksService.ensureLoaded(id);
      this.sprintsService.ensureLoaded(id);
      this.milestonesService.ensureLoaded(id);
      this.risquesService.ensureLoaded(id);
      this.recommandationsService.ensureLoaded(id);
    });
    // Le rapport « Portefeuille » lit l'avancement, les sprints et le prochain jalon des projets ; seuls ceux
    // accessibles à l'utilisateur sont chargés (les autres renverraient un refus d'accès affiché en alerte).
    effect(() => this.projectsService.chargerIndicateurs(this.projetsDisponibles().map((p) => p.id)));
  }

  protected readonly document = computed<ReportDocument>(() => {
    switch (this.selectedId()) {
      case 'sprint':
        return this.documentSprint();
      case 'risques':
        return this.documentRisques();
      case 'temps':
        return this.documentTemps();
      case 'portefeuille':
        return this.documentPortefeuille();
      case 'predictif':
        return this.documentPredictif();
      default:
        return this.documentHebdo();
    }
  });

  selectProjet(id: string): void {
    this.projetChoisi.set(id);
  }

  selectTemplate(id: string): void {
    this.selectedId.set(id);
  }

  protected readonly exportEnCours = signal(false);

  /** Télécharge le rapport affiché au format PDF. */
  async exporterPdf(): Promise<void> {
    if (this.exportEnCours()) return;
    this.exportEnCours.set(true);
    try {
      await exporterRapportPdf(`${this.document().nom} — ${this.projet()?.nom ?? 'GPDI'}`);
    } catch {
      this.toast.show({ type: 'error', message: "L'export PDF a échoué. Veuillez réessayer." });
    } finally {
      this.exportEnCours.set(false);
    }
  }

  protected readonly diffusionEnCours = signal(false);

  /** Diffuse le modèle de rapport sélectionné (photo figée) aux membres concernés du projet et au Directeur DSI. */
  async diffuser(): Promise<void> {
    const projetId = this.projetId();
    if (!projetId || this.diffusionEnCours()) return;
    this.diffusionEnCours.set(true);
    try {
      const rapport = await this.rapportsDiffusesService.diffuser(projetId, this.selectedId(), this.document());
      this.toast.show({ type: 'success', message: `« ${rapport.nom} » diffusé aux membres du projet et au Directeur DSI.` });
    } catch {
      // Message d'erreur déjà affiché par l'intercepteur HTTP global.
    } finally {
      this.diffusionEnCours.set(false);
    }
  }

  // --- Avancement hebdomadaire ---

  private documentHebdo(): ReportDocument {
    const p = this.projet();
    const taches = this.taches();
    const terminees = taches.filter((t) => t.statut === 'terminee').length;
    const bloquees = taches.filter((t) => t.statut === 'bloquee');
    const attention = this.jalons().find((j) => j.statut === 'a_risque')?.alerte;
    const sprintCourant = this.sprints().find((s) => s.statut === 'en_cours');
    const chargeSprint = sprintCourant ? this.tasksService.bySprint(sprintCourant.id)().reduce((somme, t) => somme + t.estimeHeures, 0) : 0;
    const surcapacite = sprintCourant !== undefined && chargeSprint > sprintCourant.capaciteHeures;

    return {
      nom: "Rapport d'avancement hebdomadaire",
      meta: 'généré automatiquement · diffusion hebdomadaire au comité',
      surtitre: MEF,
      titre: `Avancement — ${p?.nom ?? ''}`,
      periode: `Sprint ${p?.sprintActuelIndex ?? 0} sur ${p?.sprintTotal ?? 0}`,
      pages: 'page 1 / 1',
      chiffres: [
        kpi('Avancement', `${p?.avancement ?? 0} %`, 'progression du projet', 'succes'),
        kpi('Tâches livrées', `${terminees}`, `sur ${taches.length} tâche(s)`, 'succes'),
        kpi('Heures consommées', heures(p?.heuresConsommees ?? 0), 'cumul projet', 'alerte'),
        kpi('Risques ouverts', `${this.risquesActifs().length}`, 'suivi hebdomadaire', this.risquesActifs().length > 0 ? 'erreur' : 'succes'),
      ],
      sections: [
        {
          titre: 'Faits marquants',
          puces: [
            { texte: `${terminees} tâche(s) livrée(s) sur le projet.`, ton: 'succes' },
            {
              texte: bloquees.length > 0 ? `${bloquees.length} tâche(s) bloquée(s) : ${bloquees.map((t) => t.code).join(', ')}.` : 'Aucune tâche bloquée.',
              ton: bloquees.length > 0 ? 'erreur' : 'succes',
            },
            sprintCourant
              ? {
                  texte: surcapacite ? `${sprintCourant.nom} en surcapacité : ${chargeSprint} h engagées pour ${sprintCourant.capaciteHeures} h de capacité.` : `${sprintCourant.nom} dans sa capacité : ${chargeSprint} h engagées pour ${sprintCourant.capaciteHeures} h.`,
                  ton: surcapacite ? ('alerte' as Ton) : ('succes' as Ton),
                }
              : { texte: 'Aucun sprint en cours.', ton: 'neutre' as Ton },
          ],
        },
        {
          titre: "Points d'attention pour la direction",
          note: { texte: attention ?? 'Aucun point de blocage majeur identifié cette semaine.', alerte: !!attention },
        },
      ],
    };
  }

  // --- Bilan de sprint ---

  private sprintDeReference() {
    const actuel = this.sprints().find((s) => s.statut === 'en_cours');
    if (actuel) return actuel;
    const clotures = this.sprints()
      .filter((s) => s.statut === 'cloture')
      .sort((a, b) => b.dateFin.localeCompare(a.dateFin));
    return clotures[0];
  }

  private documentSprint(): ReportDocument {
    const p = this.projet();
    const sprint = this.sprintDeReference();
    const taches = sprint ? this.tasksService.bySprint(sprint.id)() : [];
    const chargeLivree = taches.reduce((s, t) => s + t.tempsPasseHeures, 0);
    const estimeTotal = taches.reduce((s, t) => s + t.estimeHeures, 0);
    const terminees = taches.filter((t) => t.statut === 'terminee');
    const reportees = taches.filter((t) => t.statut !== 'terminee');
    const engagementTenu = taches.length > 0 ? Math.round((terminees.length / taches.length) * 100) : 0;

    const sprintClos = sprint?.statut === 'cloture';

    return {
      nom: 'Bilan de sprint',
      meta: sprintClos ? 'généré à la clôture du sprint · automatique' : 'généré en cours de sprint · suivi intermédiaire',
      surtitre: MEF,
      titre: `Bilan du ${sprint?.nom ?? 'sprint'} — ${p?.nom ?? ''}`,
      periode: sprint ? `Du ${formatDateFr(sprint.dateDebut)} au ${formatDateFr(sprint.dateFin)}` : 'Aucun sprint suivi',
      pages: 'page 1 / 1',
      chiffres: [
        kpi('Charge livrée', heures(chargeLivree), `sur ${heures(estimeTotal)} engagées`, 'succes'),
        kpi('Engagement tenu', `${engagementTenu} %`, 'objectif 85 %', engagementTenu >= 85 ? 'succes' : 'alerte'),
        kpi('Éléments reportés', `${reportees.length}`, `sur ${taches.length} engagés`, reportees.length > 0 ? 'alerte' : 'succes'),
        kpi('Tâches bloquées', `${taches.filter((t) => t.statut === 'bloquee').length}`, sprintClos ? 'à date de clôture' : 'à date du jour', 'erreur'),
      ],
      sections: [
        {
          titre: 'Engagements du sprint',
          table: {
            colonnes: [{ libelle: 'Élément', largeur: '1.6fr' }, { libelle: 'Estimé', largeur: '0.6fr', align: 'right' }, { libelle: 'Réalisé', largeur: '0.6fr', align: 'right' }, { libelle: 'État', largeur: '0.8fr' }],
            lignes: taches.map((t) => [
              { texte: `${t.code} — ${t.titre}` },
              { texte: heures(t.estimeHeures) },
              { texte: heures(t.tempsPasseHeures) },
              { texte: STATUT_TACHE_LABEL[t.statut].label, ton: STATUT_TACHE_LABEL[t.statut].ton, accent: true },
            ]),
          },
        },
        {
          titre: sprintClos ? 'Causes de report' : 'Éléments restant à livrer',
          puces:
            reportees.length > 0
              ? reportees.slice(0, 4).map((t) => ({ texte: `${t.code} — ${t.titre} ${sprintClos ? "n'a pas été livrée sur ce sprint." : 'reste à livrer.'}`, ton: 'alerte' as Ton }))
              : [{ texte: sprintClos ? 'Tous les éléments engagés ont été livrés sur ce sprint.' : 'Tous les éléments engagés sont déjà livrés.', ton: 'succes' as Ton }],
        },
        {
          titre: sprintClos ? 'Recommandation pour le prochain sprint' : 'Recommandation pour la fin de sprint',
          note: { texte: reportees.length > 0 ? `Sécuriser en priorité les ${reportees.length} élément(s) restant à livrer avant la fin du sprint.` : "Maintenir le rythme d'engagement actuel, aucun report à traiter." },
        },
      ],
    };
  }

  // --- Registre des risques ---

  private documentRisques(): ReportDocument {
    const p = this.projet();
    const actifs = [...this.risquesActifs()].sort((a, b) => b.probabilite * b.impact - a.probabilite * a.impact);
    const clos = this.risquesTous().filter((r) => r.statut === 'clos');
    const critiqueHaute = actifs.filter((r) => {
      const c = criticiteRisque(r.probabilite, r.impact);
      return c === 'critique' || c === 'eleve';
    });
    const modere = actifs.filter((r) => criticiteRisque(r.probabilite, r.impact) === 'modere');

    return {
      nom: 'Registre des risques',
      meta: "état arrêté à ce jour · détection assistée par l'IA",
      surtitre: MEF,
      titre: `Registre des risques — ${p?.nom ?? ''}`,
      periode: `Situation actuelle · ${actifs.length} risque(s) ouvert(s)`,
      pages: 'page 1 / 1',
      chiffres: [
        kpi('Risques ouverts', `${actifs.length}`, `sur ${this.risquesTous().length} suivis`, 'erreur'),
        kpi('Criticité haute', `${critiqueHaute.length}`, 'sous surveillance', 'erreur'),
        kpi('Criticité modérée', `${modere.length}`, 'à surveiller', 'alerte'),
        kpi('Clos', `${clos.length}`, 'risques levés', 'succes'),
      ],
      sections: [
        {
          titre: 'Risques ouverts',
          table: {
            colonnes: [{ libelle: 'Risque', largeur: '1.7fr' }, { libelle: 'Score', largeur: '0.6fr', align: 'right' }, { libelle: 'Criticité', largeur: '0.8fr' }, { libelle: 'Parade retenue', largeur: '1.4fr' }, { libelle: 'Porteur', largeur: '1fr' }],
            lignes: actifs.map((r) => {
              const c = CRITICITE_LABEL[criticiteRisque(r.probabilite, r.impact)];
              return [
                { texte: `${r.code} — ${r.libelle}` },
                { texte: `${r.probabilite} × ${r.impact}` },
                { texte: c.label, ton: c.ton, accent: true },
                { texte: r.parade },
                { texte: this.peopleService.byId(r.porteurId)?.nom ?? '—' },
              ];
            }),
          },
        },
        {
          titre: 'Exposition résiduelle',
          note: {
            texte:
              critiqueHaute.length > 0
                ? `${critiqueHaute.length} risque(s) de criticité haute pèsent sur le prochain jalon${p?.prochainJalon ? ` du ${formatDateFr(p.prochainJalon)}` : ''}.`
                : 'Aucun risque de criticité haute actif sur ce projet.',
            alerte: critiqueHaute.length > 0,
          },
        },
      ],
    };
  }

  // --- Consommation des temps ---

  private documentTemps(): ReportDocument {
    const p = this.projet();
    const taches = this.taches();
    const heuresSaisies = taches.reduce((s, t) => s + t.tempsPasseHeures, 0);
    const heuresPrevues = taches.reduce((s, t) => s + t.estimeHeures, 0);
    const ecart = heuresSaisies - heuresPrevues;
    const depassements = taches.filter((t) => t.tempsPasseHeures > t.estimeHeures);

    const parAgent = new Map<string, { estime: number; passe: number }>();
    for (const t of taches) {
      for (const agentId of t.assigneIds) {
        const entree = parAgent.get(agentId) ?? { estime: 0, passe: 0 };
        entree.estime += t.estimeHeures;
        entree.passe += t.tempsPasseHeures;
        parAgent.set(agentId, entree);
      }
    }
    const lignesAgents = [...parAgent.entries()]
      .map(([id, v]) => ({ personne: this.peopleService.byId(id), ...v }))
      .filter((l) => l.personne !== undefined)
      .sort((a, b) => b.passe - a.passe);

    const topTaches = [...taches].sort((a, b) => b.tempsPasseHeures - a.tempsPasseHeures).slice(0, 4);

    return {
      nom: 'Consommation des temps',
      meta: 'généré automatiquement · saisies confirmées par le chef de projet',
      surtitre: MEF,
      titre: `Consommation des temps — ${p?.nom ?? ''}`,
      periode: `Cumul projet · ${lignesAgents.length} agent(s)`,
      pages: 'page 1 / 1',
      chiffres: [
        kpi('Durée saisies', heures(heuresSaisies), 'cumul projet', 'neutre'),
        kpi('Durée prévues', heures(heuresPrevues), 'estimations validées', 'neutre'),
        kpi('Écart', heuresSignees(ecart), ecart > 0 ? 'au-delà du prévu' : 'dans le prévu', ecart > 0 ? 'alerte' : 'succes'),
        kpi('Tâches en dépassement', `${depassements.length}`, `sur ${taches.length} tâches`, depassements.length > 0 ? 'alerte' : 'succes'),
      ],
      sections: [
        {
          titre: 'Par agent',
          table: {
            colonnes: [{ libelle: 'Agent', largeur: '1.4fr' }, { libelle: 'Prévu', largeur: '0.6fr', align: 'right' }, { libelle: 'Saisi', largeur: '0.6fr', align: 'right' }, { libelle: 'Écart', largeur: '0.6fr', align: 'right' }],
            lignes: lignesAgents.map((l) => {
              const e = l.passe - l.estime;
              return [
                { texte: l.personne!.nom },
                { texte: heures(l.estime) },
                { texte: heures(l.passe) },
                { texte: heuresSignees(e), ton: e > 0 ? 'alerte' : 'succes', accent: true },
              ];
            }),
          },
        },
        {
          titre: 'Par tâche la plus consommatrice',
          table: {
            colonnes: [{ libelle: 'Tâche', largeur: '2fr' }, { libelle: 'Estimé', largeur: '0.7fr', align: 'right' }, { libelle: 'Saisi', largeur: '0.7fr', align: 'right' }, { libelle: 'Avancement', largeur: '0.8fr', align: 'right' }],
            lignes: topTaches.map((t) => {
              const pct = t.estimeHeures > 0 ? Math.round((t.tempsPasseHeures / t.estimeHeures) * 100) : 0;
              // Un avancement ne dépasse jamais 100 % : au-delà de l'estimation, on affiche 100 % et le dépassement en heures.
              const depasse = pct > 100;
              return [
                { texte: `${t.code} — ${t.titre}` },
                { texte: heures(t.estimeHeures) },
                { texte: heures(t.tempsPasseHeures) },
                {
                  texte: depasse ? `100 % · dépassé de ${heures(t.tempsPasseHeures - t.estimeHeures)}` : `${pct} %`,
                  ton: depasse ? 'erreur' : 'succes',
                  accent: true,
                },
              ];
            }),
          },
        },
        {
          titre: 'Observations',
          puces:
            depassements.length > 0
              ? depassements
                  .slice(0, 2)
                  .map((t) => ({ texte: `La tâche ${t.code} dépasse son estimation de ${t.tempsPasseHeures - t.estimeHeures} h : réestimation à prévoir.`, ton: 'alerte' as Ton }))
              : [{ texte: 'Aucune tâche ne dépasse son estimation initiale.', ton: 'succes' as Ton }],
        },
      ],
    };
  }

  // --- Portefeuille DSI ---

  private documentPortefeuille(): ReportDocument {
    // Limité aux projets de l'utilisateur : ceux du chef de projet dans son espace, tous pour le Directeur DSI.
    const projets = this.projetsDisponibles();
    const actifs = projets.filter((p) => p.statut === 'en_cours');
    const aRisque = projets.filter((p) => p.sante === 'a_risque');
    const clotures = projets.filter((p) => p.statut === 'cloture');
    const avancementMoyen = projets.length > 0 ? Math.round(projets.reduce((s, p) => s + p.avancement, 0) / projets.length) : 0;

    return {
      nom: 'Portefeuille DSI',
      meta: 'généré automatiquement · destiné au comité de direction',
      surtitre: MEF,
      titre: `Portefeuille de la DSI — ${projets.length} projet(s)`,
      periode: 'Situation actuelle',
      pages: 'page 1 / 1',
      chiffres: [
        kpi('Projets actifs', `${actifs.length}`, `${clotures.length} clos`, 'neutre'),
        kpi('Projets à risque', `${aRisque.length}`, 'sous surveillance', aRisque.length > 0 ? 'erreur' : 'succes'),
        kpi('Avancement moyen', `${avancementMoyen} %`, 'tous projets confondus', 'succes'),
        kpi('Projets clos', `${clotures.length}`, 'archivés', 'neutre'),
      ],
      sections: [
        {
          titre: 'Projets suivis',
          table: {
            colonnes: [{ libelle: 'Projet', largeur: '1.7fr' }, { libelle: 'Chef de projet', largeur: '1fr' }, { libelle: 'Avancement', largeur: '0.7fr', align: 'right' }, { libelle: 'Prochain jalon', largeur: '0.9fr' }, { libelle: 'État', largeur: '0.9fr' }],
            lignes: projets.map((p) => {
              const s = SANTE_LABEL[p.sante];
              return [
                { texte: p.nom },
                { texte: this.peopleService.byId(p.responsableId)?.nom ?? '—' },
                { texte: `${p.avancement} %` },
                { texte: p.prochainJalon ? formatDateFr(p.prochainJalon) : '—' },
                { texte: s.label, ton: s.ton, accent: true },
              ];
            }),
          },
        },
        {
          titre: 'Lecture pour le comité',
          note: {
            texte:
              aRisque.length > 0
                ? `${aRisque.length} projet(s) concentrent l'essentiel de l'exposition du portefeuille : ${aRisque.map((p) => p.nom).join(', ')}.`
                : 'Aucun projet du portefeuille ne présente de risque actif.',
            alerte: aRisque.length > 0,
          },
        },
      ],
    };
  }

  // --- Analyse prédictive ---
  //
  // Calcul frontend a partir de la velocite reelle des sprints clotures du projet (heures de travail
  // effectivement enregistrees par sprint) : aucune donnee fabriquee, aucun appel IA. Avec moins de deux
  // sprints clotures, l'historique est jugé insuffisant pour projeter quoi que ce soit de fiable.

  private velociteParSprintClos(): number[] {
    return [...this.sprints()]
      .filter((s) => s.statut === 'cloture')
      .sort((a, b) => a.dateFin.localeCompare(b.dateFin))
      .map((sp) => this.tasksService.bySprint(sp.id)().reduce((s, t) => s + t.tempsPasseHeures, 0));
  }

  private dureeMoyenneSprintJours(): number {
    const clotures = [...this.sprints()].filter((s) => s.statut === 'cloture');
    if (clotures.length === 0) return 14;
    const durees = clotures.map((sp) => Math.max(1, joursEntre(sp.dateDebut, sp.dateFin) + 1));
    return Math.round(durees.reduce((s, d) => s + d, 0) / durees.length);
  }

  private travailRestantHeures(): number {
    return this.taches()
      .filter((t) => t.statut !== 'terminee')
      .reduce((s, t) => s + Math.max(0, t.estimeHeures - t.tempsPasseHeures), 0);
  }

  /** Projette une date de fin en appliquant une vélocité constante au travail restant, par pas de sprint. */
  private projeterFin(travailRestant: number, velocite: number, dureeSprintJours: number): string | null {
    if (travailRestant <= 0) return new Date().toISOString().slice(0, 10);
    if (velocite <= 0) return null;
    const nombreSprints = Math.ceil(travailRestant / velocite);
    return ajouterJours(new Date().toISOString().slice(0, 10), nombreSprints * dureeSprintJours);
  }

  /** Indice de confiance = régularité de la vélocité historique (coefficient de variation), pas une estimation IA. */
  private indiceConfiance(velocites: number[], moyenne: number): number | null {
    if (velocites.length < 2 || moyenne <= 0) return null;
    const variance = velocites.reduce((s, v) => s + (v - moyenne) ** 2, 0) / velocites.length;
    const coefVariation = Math.sqrt(variance) / moyenne;
    return Math.round(Math.min(95, Math.max(10, 100 - coefVariation * 100)));
  }

  private documentPredictif(): ReportDocument {
    const p = this.projet();
    const alertes = this.recommandationsService.byProject(this.projetId())();
    const clotures = [...this.sprints()].filter((s) => s.statut === 'cloture');
    const velocites = this.velociteParSprintClos();
    const moyenne = velocites.length > 0 ? velocites.reduce((s, v) => s + v, 0) / velocites.length : 0;
    const min = velocites.length > 0 ? Math.min(...velocites) : 0;
    const max = velocites.length > 0 ? Math.max(...velocites) : 0;
    const dureeSprintJours = this.dureeMoyenneSprintJours();
    const travailRestant = this.travailRestantHeures();
    const confiance = this.indiceConfiance(velocites, moyenne);
    const principale = alertes[0];

    const historiqueSuffisant = velocites.length >= 2;
    const finCentrale = historiqueSuffisant ? this.projeterFin(travailRestant, moyenne, dureeSprintJours) : null;
    const finOptimiste = historiqueSuffisant ? this.projeterFin(travailRestant, max, dureeSprintJours) : null;
    const finDegradee = historiqueSuffisant ? this.projeterFin(travailRestant, min, dureeSprintJours) : null;

    const enRetard = !!(finCentrale && p?.dateFin && finCentrale > p.dateFin);

    return {
      nom: 'Analyse prédictive',
      meta: 'calcul automatique · vélocité réelle des sprints clôturés du projet',
      surtitre: MEF,
      titre: `Projection de fin — ${p?.nom ?? ''}`,
      periode: historiqueSuffisant
        ? `Projection sur ${clotures.length} sprint(s) d'historique · ${dureeSprintJours} j/sprint en moyenne`
        : "Historique insuffisant (moins de 2 sprints clôturés)",
      pages: 'page 1 / 1',
      chiffres: [
        kpi('Fin projetée', finCentrale ? formatDateFr(finCentrale) : 'N/D', historiqueSuffisant ? (enRetard ? 'au-delà de l\'échéance cible' : 'dans l\'échéance cible') : 'historique insuffisant', historiqueSuffisant ? (enRetard ? 'erreur' : 'succes') : 'neutre'),
        kpi('Indice de confiance', confiance !== null ? `${confiance} %` : 'N/D', `régularité sur ${velocites.length} sprint(s) clos`, confiance !== null && confiance >= 60 ? 'succes' : 'alerte'),
        kpi('Alertes actives', `${alertes.length}`, alertes.length > 0 ? 'dont recommandations en attente' : 'aucune en attente', alertes.length > 0 ? 'erreur' : 'succes'),
        kpi('Vélocité moyenne', `${Math.round(moyenne)} h/sprint`, `sur ${clotures.length} sprint(s) clôturé(s)`, 'neutre'),
      ],
      sections: [
        {
          titre: 'Signaux détectés',
          puces:
            alertes.length > 0
              ? alertes.map((a) => ({ texte: a.libelle, ton: 'alerte' as Ton }))
              : [{ texte: "Aucun signal actif détecté par l'assistant sur ce projet.", ton: 'succes' as Ton }],
        },
        {
          titre: 'Scénarios de fin',
          table: historiqueSuffisant
            ? {
                colonnes: [{ libelle: 'Scénario', largeur: '1.1fr' }, { libelle: 'Hypothèse', largeur: '2fr' }, { libelle: 'Fin estimée', largeur: '0.9fr' }, { libelle: 'Vélocité retenue', largeur: '0.9fr', align: 'right' }],
                lignes: [
                  [{ texte: 'Optimiste', ton: 'succes', accent: true }, { texte: `Rythme du meilleur sprint observé (${Math.round(max)} h) maintenu jusqu'à la fin.` }, { texte: finOptimiste ? formatDateFr(finOptimiste) : 'N/D' }, { texte: `${Math.round(max)} h/sprint` }],
                  [{ texte: 'Central', ton: 'alerte', accent: true }, { texte: `Vélocité moyenne observée (${Math.round(moyenne)} h) maintenue jusqu'à la fin.` }, { texte: finCentrale ? formatDateFr(finCentrale) : 'N/D' }, { texte: `${Math.round(moyenne)} h/sprint` }],
                  [{ texte: 'Dégradé', ton: 'erreur', accent: true }, { texte: `Rythme du sprint le plus faible observé (${Math.round(min)} h) maintenu jusqu'à la fin.` }, { texte: finDegradee ? formatDateFr(finDegradee) : 'N/D' }, { texte: `${Math.round(min)} h/sprint` }],
                ],
              }
            : undefined,
          note: historiqueSuffisant
            ? undefined
            : { texte: `${clotures.length} sprint(s) clôturé(s) sur ce projet : au moins 2 sont nécessaires pour projeter une date de fin à partir de la vélocité réelle.`, alerte: true },
        },
        {
          titre: "Action recommandée",
          note: {
            texte: principale ? `${principale.libelle} — ${principale.justification}` : "Aucune action corrective recommandée à ce stade : le projet suit sa trajectoire prévue.",
            alerte: !!principale,
          },
        },
      ],
    };
  }
}

/** Nombre de jours calendaires entre deux dates ISO (yyyy-MM-dd), bornes incluses côté fin. */
function joursEntre(debutIso: string, finIso: string): number {
  const debut = new Date(debutIso).getTime();
  const fin = new Date(finIso).getTime();
  return Math.round((fin - debut) / (1000 * 60 * 60 * 24));
}

function ajouterJours(dateIso: string, jours: number): string {
  const date = new Date(dateIso);
  date.setDate(date.getDate() + jours);
  return date.toISOString().slice(0, 10);
}
