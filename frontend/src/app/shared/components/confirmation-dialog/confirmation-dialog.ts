import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { Modal } from '@shared/components/modal/modal';
import { Button } from '@shared/components/button/button';

@Component({
  selector: 'app-confirmation-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Modal, Button],
  template: `
    <app-modal [open]="open()" [width]="420" [ariaLabel]="title()" (close)="cancel.emit()">
      <div class="confirmation-dialog">
        <h2 class="confirmation-dialog__title">{{ title() }}</h2>
        <p class="confirmation-dialog__message">{{ message() }}</p>
        <div class="confirmation-dialog__actions">
          <button app-button variant="secondary" type="button" (click)="cancel.emit()">{{ cancelLabel() }}</button>
          <button app-button [variant]="destructive() ? 'destructive' : 'primary'" type="button" (click)="confirm.emit()">
            {{ confirmLabel() }}
          </button>
        </div>
      </div>
    </app-modal>
  `,
  styles: `
    .confirmation-dialog {
      display: flex;
      flex-direction: column;
      gap: 14px;
      padding: 24px;
    }
    .confirmation-dialog__title {
      margin: 0;
      font-size: 18px;
      font-weight: 700;
    }
    .confirmation-dialog__message {
      margin: 0;
      font-size: 13.5px;
      line-height: 1.55;
      color: var(--color-text-secondary);
    }
    .confirmation-dialog__actions {
      display: flex;
      justify-content: flex-end;
      gap: 10px;
      margin-top: 6px;
    }
  `,
})
export class ConfirmationDialog {
  readonly open = input.required<boolean>();
  readonly title = input<string>('Confirmer');
  readonly message = input<string>('Cette action est-elle confirmée ?');
  readonly confirmLabel = input<string>('Confirmer');
  readonly cancelLabel = input<string>('Annuler');
  readonly destructive = input<boolean>(false);
  readonly confirm = output<void>();
  readonly cancel = output<void>();
}
