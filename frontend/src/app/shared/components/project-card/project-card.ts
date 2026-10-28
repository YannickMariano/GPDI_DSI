import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { NgTemplateOutlet } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Project } from '@core/models/project.model';
import { Person } from '@core/models/person.model';
import { SANTE_CONFIG } from '@shared/utils/sante-config';
import { formatDateFr, formatHours } from '@shared/utils/format.util';
import { ProgressBar } from '@shared/components/progress-bar/progress-bar';
import { Avatar } from '@shared/components/avatar/avatar';

@Component({
  selector: 'app-project-card',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, NgTemplateOutlet, ProgressBar, Avatar],
  host: { class: 'project-card', '[style.borderTopColor]': 'sante().border' },
  templateUrl: './project-card.html',
  styleUrl: './project-card.css',
})
export class ProjectCard {
  readonly project = input.required<Project>();
  readonly responsable = input<Person | undefined>(undefined);
  readonly clickable = input<boolean>(true);
  /** N'affiche le délai avant la fin du projet (ou le retard) que lorsque la page appelante le demande explicitement. */
  readonly showJoursRestants = input<boolean>(false);
  /** Masque le sous-titre (direction) uniquement lorsque la page appelante le demande explicitement. */
  readonly showDirection = input<boolean>(true);
  /** Badge de statut (En cours, Archivé…) affiché sous le titre, uniquement si fourni par la page appelante. */
  readonly statutBadge = input<{ label: string; bg: string; text: string } | undefined>(undefined);

  protected readonly sante = computed(() => SANTE_CONFIG[this.project().sante]);
  /** Heures consommées arrondies à l'heure entière (pas de décimale). */
  protected readonly heuresConsommees = computed(() => formatHours(Math.round(this.project().heuresConsommees)));
  protected readonly formatDateFr = formatDateFr;

  /** "19 jours restants avant la fin du projet" une fois la date de fin dépassée : "Date de fin dépassée de 5 jours". */
  protected readonly joursRestantsLabel = computed(() => {
    const dateFin = this.project().dateFin;
    if (!dateFin) return null;
    const fin = new Date(`${dateFin}T23:59:59`);
    const jours = Math.ceil((fin.getTime() - Date.now()) / (1000 * 60 * 60 * 24));
    if (jours > 0) return `${jours} jour${jours > 1 ? 's' : ''} restant${jours > 1 ? 's' : ''} avant la fin du projet`;
    if (jours === 0) return 'Dernier jour du projet';
    const retard = Math.abs(jours);
    return `Date de fin dépassée de ${retard} jour${retard > 1 ? 's' : ''}`;
  });

  protected readonly enRetard = computed(() => {
    const dateFin = this.project().dateFin;
    if (!dateFin) return false;
    return new Date(`${dateFin}T23:59:59`).getTime() < Date.now();
  });
}
