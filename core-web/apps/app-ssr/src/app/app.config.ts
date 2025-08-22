import { provideHttpClient, withFetch } from '@angular/common/http';
import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideClientHydration, withEventReplay } from '@angular/platform-browser';
import { provideRouter } from '@angular/router';
import { provideDotCMSClient } from '@dotcms/angular';
import { appRoutes } from './app.routes';
import { withHttpTransferCacheOptions } from './transfer_cache';

export const appConfig: ApplicationConfig = {
    providers: [
        provideZoneChangeDetection({ eventCoalescing: true }),
        provideRouter(appRoutes),
        provideHttpClient(withFetch()),
        provideClientHydration(
            withEventReplay(),
            withHttpTransferCacheOptions({
                includePostRequests: true,
                includeRequestsWithAuthHeaders: true
            })
        ),
        provideDotCMSClient({
            dotcmsUrl: 'http://localhost:8080',
            authToken:
                'eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhcGlmZDc5NmNkNi0xNmFjLTQ4OTEtYWE1YS0yMDJiMzY0N2Q5MTUiLCJ4bW9kIjoxNzU1MDIxNjkwMDAwLCJuYmYiOjE3NTUwMjE2OTAsImlzcyI6IjE5NGY5NzZhNjUiLCJsYWJlbCI6ImRlbW8iLCJleHAiOjE4NDk2NjU2MDAsImlhdCI6MTc1NTAyMTY5MCwianRpIjoiNWMzMjcwM2MtZjVjYS00ZDE3LWIzOWQtZmEzZmMyYWQyMTEzIn0.gKv_bXg-fMvbWmTVTvecaikG1saNP8fnF4j0aEi4--E',
            siteId: 'YOUR_SITE_ID'
        })
    ]
};
