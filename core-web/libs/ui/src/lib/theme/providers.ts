import { EnvironmentProviders } from '@angular/core';

import { providePrimeNG } from 'primeng/config';

import { CustomLaraPreset } from './theme.config';

/**
 * Provides PrimeNG configuration with the custom Lara theme preset.
 * This is the centralized theme provider for all dotCMS applications.
 *
 * @param options - Optional PrimeNG configuration options
 * @returns EnvironmentProviders for use in ApplicationConfig (standalone apps)
 *
 * @example
 * ```typescript
 * export const appConfig: ApplicationConfig = {
 *   providers: [
 *     provideDotCMSTheme(),
 *     // other providers
 *   ]
 * };
 * ```
 */
export function provideDotCMSTheme(options?: {
    darkModeSelector?: string | false;
}): EnvironmentProviders {
    return providePrimeNG({
        theme: {
            preset: CustomLaraPreset,
            options: {
                darkModeSelector: options?.darkModeSelector ?? false
            }
        }
    });
}
