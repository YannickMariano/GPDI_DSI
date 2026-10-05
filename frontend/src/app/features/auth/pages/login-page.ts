import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { AuthService } from '@core/services/auth.service';
import { defaultRouteForRole } from '@core/models/role.model';
import { Button } from '@shared/components/button/button';

@Component({
  selector: 'app-login-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, Button],
  templateUrl: './login-page.html',
  styleUrl: './login-page.css',
})
export class LoginPage {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly showPassword = signal(false);
  protected readonly isSubmitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    identifiant: ['', [Validators.required]],
    motDePasse: ['', [Validators.required]],
    gardeSession: [false],
  });

  protected togglePasswordVisibility(): void {
    this.showPassword.update((v) => !v);
  }

  protected async submit(): Promise<void> {
    if (this.form.invalid || this.isSubmitting()) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set(null);

    const { identifiant, motDePasse, gardeSession } = this.form.getRawValue();
    const result = await this.authService.login({ identifiant, motDePasse, gardeSession });
    this.isSubmitting.set(false);

    if (!result.success || !result.user) {
      this.errorMessage.set(result.message ?? 'Identifiants incorrects.');
      return;
    }

    // returnUrl vaut "/" quand le garde d'authentification a intercepté une visite de la racine :
    // ce n'est pas une destination précise à restaurer, y naviguer ferait juste rebondir sur
    // HomeRedirectPage pour un second saut vers ce même defaultRouteForRole.
    const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
    const destination = returnUrl && returnUrl !== '/' ? returnUrl : defaultRouteForRole(result.user.role);
    this.router.navigateByUrl(destination);
  }
}
