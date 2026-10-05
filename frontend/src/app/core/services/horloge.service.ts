import { Injectable, signal } from '@angular/core';

const INTERVALLE_RAFRAICHISSEMENT_MS = 1_000;

/** Horodatage partagé, rafraîchi périodiquement — permet aux vues d'afficher un temps écoulé qui avance sans appel serveur. */
@Injectable({ providedIn: 'root' })
export class HorlogeService {
  private readonly _maintenant = signal(Date.now());
  readonly maintenant = this._maintenant.asReadonly();

  constructor() {
    setInterval(() => this._maintenant.set(Date.now()), INTERVALLE_RAFRAICHISSEMENT_MS);
  }
}
