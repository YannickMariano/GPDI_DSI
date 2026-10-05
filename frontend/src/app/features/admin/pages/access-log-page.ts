import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { PeopleService } from '@core/services/people.service';
import { JournalConnexionEntry, JournalConnexionService } from '@core/services/journal-connexion.service';
import { Person } from '@core/models/person.model';
import { Avatar } from '@shared/components/avatar/avatar';
import { ToastService } from '@shared/components/toast/toast.service';

interface JournalRow {
  entry: JournalConnexionEntry;
  personne?: Person;
}

@Component({
  selector: 'app-access-log-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Avatar],
  templateUrl: './access-log-page.html',
  styleUrl: './access-log-page.css',
})
export class AccessLogPage {
  private readonly journalConnexionService = inject(JournalConnexionService);
  private readonly peopleService = inject(PeopleService);
  private readonly toast = inject(ToastService);

  // Une tentative de connexion avec un matricule inconnu (typo, compte inexistant) doit rester
  // visible dans le journal — on ne filtre jamais une ligne faute de compte GPDI résolu.
  protected readonly journal = computed<JournalRow[]>(() =>
    this.journalConnexionService.evenements().map((entry) => ({
      entry,
      personne: this.peopleService.byMatricule(entry.matricule),
    })),
  );

  exporterJournal(): void {
    this.toast.show({ type: 'info', message: 'Export du journal des connexions — à venir.' });
  }

  formatHorodatage(iso: string): string {
    const date = new Date(iso);
    const jour = date.toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit' });
    const heure = date.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
    return `${jour} ${heure}`;
  }
}
