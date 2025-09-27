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
import { routes } from './app.routes';

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
                    httpClient: options.httpClient ? options.httpClient : httpClient
                });

                return dotCMSClient;
            }
        }
    ]);
}

export const appConfig: ApplicationConfig = {
    providers: [
        provideZoneChangeDetection({ eventCoalescing: true }),
        provideRouter(routes),
        provideHttpClient(),
        provideClientHydration(
            withEventReplay(),
            withHttpTransferCacheOptions({
                includePostRequests: true,
                includeRequestsWithAuthHeaders: true
            })
        ),
        provideDotCMSClient({
            dotcmsUrl: 'https://demo.dotcms.com',
            authToken:
                'eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhcGliNGM2ODJkOS1hNzQ4LTRjM2EtOWE1ZS0zZmI3NTg5MDkzYWYiLCJ4bW9kIjoxNzU2MTYyMzQ2MDAwLCJuYmYiOjE3NTYxNjIzNDYsImlzcyI6IjU3ZjQ5OGFlMGYiLCJleHAiOjE3NTcwMjYzNDYsImlhdCI6MTc1NjE2MjM0NiwianRpIjoiNTMwOWQxMzktNjEyNC00NWE5LTliOTItNDYyMDBjZDMzZjA4In0.GUqyl8vIy8cKB6r7UwUfTNzXfpIswUcn4fg_KbX03I8',
            siteId: 'YOUR_SITE_ID'
        })
    ]
};
