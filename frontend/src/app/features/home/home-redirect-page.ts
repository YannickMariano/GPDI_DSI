import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '@core/services/auth.service';
import { defaultRouteForRole } from '@core/models/role.model';

@Component({
  selector: 'app-home-redirect-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: ``,
})
export class HomeRedirectPage {
  constructor() {
    const router = inject(Router);
    const user = inject(AuthService).currentUser();
    router.navigateByUrl(user ? defaultRouteForRole(user.role) : '/login', { replaceUrl: true });
  }
}
