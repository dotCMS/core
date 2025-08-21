import { HttpClient, provideHttpClient, withFetch } from '@angular/common/http';
import {
    ApplicationConfig,
    provideZoneChangeDetection,
    makeEnvironmentProviders,
    EnvironmentProviders,
    inject
} from '@angular/core';
import {
    provideClientHydration,
    withEventReplay,
    withHttpTransferCacheOptions
} from '@angular/platform-browser';
import { provideRouter } from '@angular/router';

import { DotCMSClient } from '@dotcms/angular';
import { createDotCMSClient } from '@dotcms/client';

import { AngularHttpClient } from './angular-httpclient';
import { appRoutes } from './app.routes';

function provideDotCMSClient(options: any): EnvironmentProviders {
    return makeEnvironmentProviders([
        {
            provide: DotCMSClient,
            useFactory: () => {
                const http = inject(HttpClient);
                const httpClient = new AngularHttpClient(http);
                const dotCMSClient = createDotCMSClient({
                    dotcmsUrl: options.dotcmsUrl,
                    authToken: options.authToken,
                    siteId: options.siteId,
                    httpClient: httpClient
                });

                return new DotCMSClient(dotCMSClient);
            }
        }
    ]);
}

export const appConfig: ApplicationConfig = {
    providers: [
        provideZoneChangeDetection({ eventCoalescing: true }),
        provideRouter(appRoutes),
        provideHttpClient(withFetch()),
        provideClientHydration(
            withEventReplay(),
            withHttpTransferCacheOptions({
                includePostRequests: true
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
