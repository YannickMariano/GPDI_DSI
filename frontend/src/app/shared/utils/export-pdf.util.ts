const MARGE_MM = 10;
const ECHELLE_CAPTURE = 2;

/**
 * Exporte en PDF (téléchargement direct, format A4 portrait) le rapport affiché, repéré par la classe
 * `.rapport-imprimable` (cf. ReportDocumentView). Le rapport est capturé en image puis découpé en pages
 * si besoin. jsPDF et html2canvas ne sont chargés qu'au premier export, pour ne pas alourdir l'application.
 */
export async function exporterRapportPdf(nomFichier: string): Promise<void> {
  const element = document.querySelector<HTMLElement>('.rapport-imprimable');
  if (!element) throw new Error('Aucun rapport à exporter.');

  const [{ jsPDF }, { default: html2canvas }] = await Promise.all([import('jspdf'), import('html2canvas')]);

  const canvas = await html2canvas(element, { scale: ECHELLE_CAPTURE, backgroundColor: '#ffffff', useCORS: true });
  const pdf = new jsPDF({ orientation: 'portrait', unit: 'mm', format: 'a4' });

  const largeurUtile = pdf.internal.pageSize.getWidth() - 2 * MARGE_MM;
  const hauteurUtile = pdf.internal.pageSize.getHeight() - 2 * MARGE_MM;
  // Hauteur (en pixels de la capture) correspondant à une page A4 utile.
  const hauteurPagePx = Math.floor((hauteurUtile * canvas.width) / largeurUtile);

  for (let debut = 0, page = 0; debut < canvas.height; debut += hauteurPagePx, page++) {
    const hauteurTranchePx = Math.min(hauteurPagePx, canvas.height - debut);
    const tranche = document.createElement('canvas');
    tranche.width = canvas.width;
    tranche.height = hauteurTranchePx;
    tranche.getContext('2d')?.drawImage(canvas, 0, debut, canvas.width, hauteurTranchePx, 0, 0, canvas.width, hauteurTranchePx);

    if (page > 0) pdf.addPage();
    pdf.addImage(tranche.toDataURL('image/png'), 'PNG', MARGE_MM, MARGE_MM, largeurUtile, (hauteurTranchePx * largeurUtile) / canvas.width);
  }

  pdf.save(`${nettoyerNomFichier(nomFichier)}.pdf`);
}

/** Retire les caractères interdits dans un nom de fichier (Windows notamment). */
function nettoyerNomFichier(nom: string): string {
  return nom.replace(/[\\/:*?"<>|]+/g, '-').trim() || 'rapport';
}
