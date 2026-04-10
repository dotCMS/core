import Lara from '@primeuix/themes/lara';

import { provideHttpClient } from '@angular/common/http';
import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';

import { providePrimeNG } from 'primeng/config';

/**
 * PrimeNG is required for components used inside `@dotcms/new-block-editor` (e.g. DataView in
 * image/video dotCMS pickers). Theme + cssLayer order must match `apps/dotcms-block-editor/src/styles.css`.
 */
export const appConfig: ApplicationConfig = {
    providers: [
        provideBrowserGlobalErrorListeners(),
        provideHttpClient(),
        provideAnimationsAsync(),
        providePrimeNG({
            theme: {
                preset: Lara,
                options: {
                    darkModeSelector: '.dark',
                    cssLayer: {
                        name: 'primeng',
                        order: 'tailwind-base, primeng, tailwind-utilities'
                    }
                }
            }
        })
    ]
};
