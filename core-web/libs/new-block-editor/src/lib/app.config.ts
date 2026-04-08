import Lara from '@primeuix/themes/lara';

import { provideHttpClient } from '@angular/common/http';
import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';

import { providePrimeNG } from 'primeng/config';

import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
    providers: [
        provideBrowserGlobalErrorListeners(),
        provideHttpClient(),
        provideRouter(routes),
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
