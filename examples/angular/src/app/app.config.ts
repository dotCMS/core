import { ApplicationConfig } from '@angular/core';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { dotcmsClientProvider } from './lib/dotcms-client-token';

const DOTCMS_CLIENT_CONFIG = {
  dotcmsUrl: "http://localhost:8080",
  authToken: "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhcGliNGJkOWNmOS01YTJmLTRmMjItYTE5NS03NjhlMzQ4ZWM1NmYiLCJ4bW9kIjoxNzE1MzUyMDAxMDAwLCJuYmYiOjE3MTUzNTIwMDEsImlzcyI6ImY4MzU2YzI2YzciLCJsYWJlbCI6IkVNQSIsImV4cCI6MTgwOTkyMTYwMCwiaWF0IjoxNzE1MzUyMDAxLCJqdGkiOiIwYjEzMDUxZi01ZWQ4LTQ5NzAtOGEyNy0yYjZkMDAzN2E0YjYifQ.FDiB-lpyvj5dEvmyhJJljIz-Hr-IkF0e7EphK8lBmSk",
  siteId: '59bb8831-6706-4589-9ca0-ff74016e02b2',
  requestOptions: {
      // In production you might want to deal with this differently
      // cache: 'no-cache'
  }
}

export const appConfig: ApplicationConfig = {
  providers: [provideRouter(routes), dotcmsClientProvider(DOTCMS_CLIENT_CONFIG)]
};
