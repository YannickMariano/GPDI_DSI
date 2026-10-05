import { Directive, ElementRef, inject, output } from '@angular/core';

@Directive({
  selector: '[appClickOutside]',
  host: {
    '(document:click)': 'onDocumentClick($event)',
  },
})
export class ClickOutside {
  private readonly elementRef = inject(ElementRef<HTMLElement>);
  readonly appClickOutside = output<void>();

  protected onDocumentClick(event: MouseEvent): void {
    if (!this.elementRef.nativeElement.contains(event.target as Node)) {
      this.appClickOutside.emit();
    }
  }
}
