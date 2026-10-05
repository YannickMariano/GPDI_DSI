import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { Avatar } from './avatar';

export interface AvatarGroupItem {
  initiales: string;
  bg: string;
  nom?: string;
}

@Component({
  selector: 'app-avatar-group',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Avatar],
  host: { class: 'avatar-group' },
  template: `
    @for (item of items(); track item.initiales + item.nom) {
      <app-avatar [initiales]="item.initiales" [bg]="item.bg" [nom]="item.nom" [size]="size()" />
    }
  `,
  styles: `
    :host {
      display: flex;
      align-items: center;
      gap: -6px;
    }
    app-avatar {
      margin-left: -6px;
      border: 2px solid var(--color-surface);
    }
    app-avatar:first-child {
      margin-left: 0;
    }
  `,
})
export class AvatarGroup {
  readonly items = input.required<AvatarGroupItem[]>();
  readonly size = input<number>(24);
}
