import { HttpClient, provideHttpClient, withFetch } from '@angular/common/http';
import {
    ApplicationConfig,
    provideBrowserGlobalErrorListeners,
    provideZoneChangeDetection
} from '@angular/core';
import {
    provideClientHydration,
    withEventReplay,
    withHttpTransferCacheOptions
} from '@angular/platform-browser';
import { provideRouter } from '@angular/router';

import { provideDotCMSImageLoader, provideDotCMSClient } from '@dotcms/angular';

import { AngularHttpClient } from './angular-httpclient';
import { appRoutes } from './app.routes';

export const appConfig: ApplicationConfig = {
    providers: [
        provideDotCMSImageLoader('https://demo.dotcms.com'),
        provideHttpClient(withFetch()),
        provideBrowserGlobalErrorListeners(),
        provideZoneChangeDetection({ eventCoalescing: true }),
        provideRouter(appRoutes),
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
                'eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhcGk0NmRiNjExMi01YTQxLTRlYTQtODEzNC04ZDEzMDA4NTA0Y2QiLCJ4bW9kIjoxNzU4NjM5ODk5MDAwLCJuYmYiOjE3NTg2Mzk4OTksImlzcyI6ImQ2MWM5Y2I2OTAiLCJsYWJlbCI6InRva2VuIiwiZXhwIjoxODUzMzAxNjAwLCJpYXQiOjE3NTg2Mzk4OTksImp0aSI6IjJhYzE3MGQ5LWE5OTQtNGNjNC04YmY0LTkzNjRmN2UzYzRiZCJ9.IfYSpCEv0sAYWWjWSK96norKYSUvRvqwJWHyRosei9k',
            siteId: 'YOUR_SITE_ID',
            httpClient: (http: HttpClient) => new AngularHttpClient(http)
        })
    ]
};
