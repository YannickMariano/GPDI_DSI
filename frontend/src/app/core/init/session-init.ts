import { EnvironmentProviders, inject, provideAppInitializer } from '@angular/core';
import { AuthService } from '@core/services/auth.service';

export function provideSessionInit(): EnvironmentProviders {
  return provideAppInitializer(() => inject(AuthService).restoreFromStorage());
}
