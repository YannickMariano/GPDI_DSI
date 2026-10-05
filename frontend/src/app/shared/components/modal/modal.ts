import { AfterViewChecked, ChangeDetectionStrategy, Component, ElementRef, input, output, viewChild } from '@angular/core';

@Component({
  selector: 'app-modal',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '(keydown.escape)': 'close.emit()',
  },
  template: `
    @if (open()) {
      <div class="modal__backdrop" (click)="close.emit()"></div>
      <div class="modal__wrapper">
        <div #panel class="modal" [style.width.px]="width()" role="dialog" aria-modal="true" [attr.aria-label]="ariaLabel()" tabindex="-1">
          <ng-content />
        </div>
      </div>
    }
  `,
  styles: `
    .modal__backdrop {
      position: fixed;
      inset: 0;
      background: rgba(15, 23, 41, 0.32);
      z-index: 40;
    }
    .modal__wrapper {
      position: fixed;
      inset: 0;
      z-index: 41;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 20px;
    }
    .modal {
      background: var(--color-surface);
      border-radius: var(--radius-card);
      box-shadow: var(--shadow-elevated);
      max-width: 100%;
      max-height: 90vh;
      overflow: auto;
      display: flex;
      flex-direction: column;
    }
  `,
})
export class Modal implements AfterViewChecked {
  readonly open = input.required<boolean>();
  readonly width = input<number>(480);
  readonly ariaLabel = input<string>('Fenêtre modale');
  readonly close = output<void>();

  private readonly panelRef = viewChild<ElementRef<HTMLElement>>('panel');
  private focusedOnOpen = false;

  ngAfterViewChecked(): void {
    const el = this.panelRef()?.nativeElement;
    if (el && this.open() && !this.focusedOnOpen) {
      el.focus();
      this.focusedOnOpen = true;
    } else if (!this.open()) {
      this.focusedOnOpen = false;
    }
  }
}
