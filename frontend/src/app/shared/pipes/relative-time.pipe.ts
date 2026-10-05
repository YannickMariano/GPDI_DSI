import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'relativeTime', pure: true })
export class RelativeTimePipe implements PipeTransform {
  transform(iso: string | undefined): string {
    if (!iso) return '';
    const diffMs = Date.now() - new Date(iso).getTime();
    const diffMin = Math.round(diffMs / 60000);

    if (diffMin < 1) return "à l'instant";
    if (diffMin < 60) return `il y a ${diffMin} min`;
    const diffH = Math.round(diffMin / 60);
    if (diffH < 24) return `il y a ${diffH} h`;
    const diffJ = Math.round(diffH / 24);
    if (diffJ === 1) return 'hier';
    if (diffJ < 7) return `il y a ${diffJ} jours`;
    return new Date(iso).toLocaleDateString('fr-FR', { day: '2-digit', month: 'short' });
  }
}
