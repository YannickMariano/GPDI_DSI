import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { PeopleService } from '@core/services/people.service';
import { ProjetsReferenceService } from '@core/services/projets-reference.service';
import { ClientShareRecord, PartagesClientService } from '@core/services/partages-client.service';
import { Person } from '@core/models/person.model';
import { Role } from '@core/models/role.model';
import { Avatar } from '@shared/components/avatar/avatar';
import { Modal } from '@shared/components/modal/modal';
import { ToastService } from '@shared/components/toast/toast.service';

interface PartageRow {
  share: ClientShareRecord;
  client: Person;
}

@Component({
  selector: 'app-client-shares-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Avatar, Modal, ReactiveFormsModule],
  templateUrl: './client-shares-page.html',
  styleUrl: './client-shares-page.css',
})
export class ClientSharesPage {
  private readonly fb = inject(FormBuilder);
  private readonly peopleService = inject(PeopleService);
  private readonly projetsReferenceService = inject(ProjetsReferenceService);
  private readonly partagesClientService = inject(PartagesClientService);
  private readonly toast = inject(ToastService);

  protected readonly clients = computed(() => this.peopleService.people().filter((p) => p.role === Role.DirectionMetier));
  protected readonly projects = this.projetsReferenceService.projets;

  protected readonly partages = computed<PartageRow[]>(() => {
    const rows: PartageRow[] = [];
    for (const share of this.partagesClientService.shares()) {
      const client = this.peopleService.byId(share.clientId);
      if (client) rows.push({ share, client });
    }
    return rows;
  });

  protected readonly modalOuvert = signal(false);

  protected readonly form = this.fb.nonNullable.group({
    clientId: ['', Validators.required],
    projectId: ['', Validators.required],
    echeance: ['', Validators.required],
  });

  async revoquer(shareId: string): Promise<void> {
    try {
      await this.partagesClientService.revoke(shareId);
      this.toast.show({ type: 'success', message: 'Partage révoqué.' });
    } catch {
      // Message d'erreur déjà affiché par l'intercepteur HTTP global.
    }
  }

  ouvrirPartage(): void {
    this.form.reset({ clientId: '', projectId: '', echeance: '' });
    this.modalOuvert.set(true);
  }

  fermerPartage(): void {
    this.modalOuvert.set(false);
  }

  async confirmerPartage(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { clientId, projectId, echeance } = this.form.getRawValue();
    const client = this.peopleService.byId(clientId);

    try {
      await this.partagesClientService.create({ clientId, projectIds: [projectId], echeance });
      this.toast.show({ type: 'success', message: `Projet partagé avec ${client?.nom ?? 'le compte client'}.` });
      this.modalOuvert.set(false);
    } catch {
      // Message d'erreur déjà affiché par l'intercepteur HTTP global (ex. partage déjà actif).
    }
  }
}
