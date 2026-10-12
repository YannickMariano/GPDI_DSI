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

  protected readonly sante = computed(() => SANTE_CONFIG[this.project().sante]);
  /** Heures consommées arrondies à l'heure entière (pas de décimale). */
  protected readonly heuresConsommees = computed(() => formatHours(Math.round(this.project().heuresConsommees)));
  protected readonly formatDateFr = formatDateFr;
}
