import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { ReportColonne, ReportDocument, Ton } from '@core/models/report.model';

const TON_COLOR: Record<Ton, string> = {
  succes: 'var(--color-success-text)',
  alerte: 'var(--color-alert)',
  erreur: 'var(--color-error-text)',
  neutre: 'var(--color-text-tertiary)',
};

/**
 * Mise en page d'un rapport (en-tête, chiffres clés, sections, pied de page) — partagée par le
 * générateur de rapports du chef de projet et la consultation des rapports diffusés (lecture seule).
 * Porte la classe `rapport-imprimable` : c'est l'élément capturé par « Exporter PDF » (cf. export-pdf.util.ts).
 */
@Component({
  selector: 'app-report-document',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './report-document.html',
  styleUrl: './report-document.css',
})
export class ReportDocumentView {
  readonly doc = input.required<ReportDocument>();

  protected readonly tonColor = TON_COLOR;

  colonnesGrid(colonnes: ReportColonne[]): string {
    return colonnes.map((c) => c.largeur).join(' ');
  }
}
