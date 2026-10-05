/** Palette d'accent pour les avatars (cartes personnes, en-têtes, filtres). */
export const AVATAR_COLORS = ['#2a5bd7', '#6941c6', '#17876d', '#b45309', '#c0392b', '#475467', '#0f6b52'];

/** Couleur d'avatar déterministe à partir d'une clé stable (matricule, id) : même clé → même couleur, sans état à persister. */
export function avatarColorFor(key: string): string {
  let hash = 0;
  for (let i = 0; i < key.length; i++) {
    hash = (hash * 31 + key.charCodeAt(i)) | 0;
  }
  return AVATAR_COLORS[Math.abs(hash) % AVATAR_COLORS.length];
}
