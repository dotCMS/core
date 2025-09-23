import { ApplicationConfig, provideBrowserGlobalErrorListeners, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { provideClientHydration, withEventReplay } from '@angular/platform-browser';
import { provideDotCMSClient } from '@dotcms/angular';
import { HttpClient, provideHttpClient, withFetch } from '@angular/common/http';
import { AngularHttpClient } from './angular-httpclient';
export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideHttpClient(withFetch()),
    provideRouter(routes), provideClientHydration(withEventReplay()),
    provideDotCMSClient({
      dotcmsUrl: 'http://localhost:8080',
      authToken:
          'eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhcGk0NmRiNjExMi01YTQxLTRlYTQtODEzNC04ZDEzMDA4NTA0Y2QiLCJ4bW9kIjoxNzU4NjM5ODk5MDAwLCJuYmYiOjE3NTg2Mzk4OTksImlzcyI6ImQ2MWM5Y2I2OTAiLCJsYWJlbCI6InRva2VuIiwiZXhwIjoxODUzMzAxNjAwLCJpYXQiOjE3NTg2Mzk4OTksImp0aSI6IjJhYzE3MGQ5LWE5OTQtNGNjNC04YmY0LTkzNjRmN2UzYzRiZCJ9.IfYSpCEv0sAYWWjWSK96norKYSUvRvqwJWHyRosei9k',
      siteId: 'YOUR_SITE_ID',
       httpClient: (http: HttpClient) => new AngularHttpClient(http)
  })
  ]
};
