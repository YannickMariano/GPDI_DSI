import { AfterViewChecked, ChangeDetectionStrategy, Component, ElementRef, input, output, viewChild } from '@angular/core';

@Component({
  selector: 'app-side-panel',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '(keydown.escape)': 'close.emit()',
  },
  template: `
    @if (open()) {
      <div class="side-panel__backdrop" (click)="close.emit()"></div>
      <div
        #panel
        class="side-panel"
        [style.width.px]="width()"
        role="dialog"
        aria-modal="true"
        [attr.aria-label]="ariaLabel()"
        tabindex="-1"
      >
        <ng-content />
      </div>
    }
  `,
  styles: `
    .side-panel__backdrop {
      position: fixed;
      inset: 0;
      background: rgba(15, 23, 41, 0.28);
      z-index: 30;
    }
    .side-panel {
      position: fixed;
      top: 0;
      right: 0;
      bottom: 0;
      background: var(--color-surface);
      border-left: 1px solid var(--color-border-strong);
      box-shadow: -8px 0 28px rgba(15, 23, 41, 0.14);
      display: flex;
      flex-direction: column;
      z-index: 31;
      max-width: 100vw;
    }
  `,
})
export class SidePanel implements AfterViewChecked {
  readonly open = input.required<boolean>();
  readonly width = input<number>(496);
  readonly ariaLabel = input<string>('Panneau de détail');
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
