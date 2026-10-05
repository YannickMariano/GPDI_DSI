import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '@core/services/auth.service';

@Component({
  selector: 'app-pilotage-redirect-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: ``,
})
export class PilotageRedirectPage {
  constructor() {
    const router = inject(Router);
    const user = inject(AuthService).currentUser();
    const projetId = user?.projetActifId;
    router.navigateByUrl(projetId ? `/projets/${projetId}/apercu` : '/projets', { replaceUrl: true });
  }
}
