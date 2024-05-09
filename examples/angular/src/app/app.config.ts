import { ApplicationConfig } from '@angular/core';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { dotcmsClientProvider } from './lib/dotcms-client-token';

const DOTCMS_CLIENT_CONFIG = {
  dotcmsUrl: "http://localhost:8080",
  authToken: "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhcGkxZmMwOGQ4My0zMGY2LTRmMTYtYmY0YS1lMGYzOGNiMzk2ZDUiLCJ4bW9kIjoxNzE1Mjg0MjQ4MDAwLCJuYmYiOjE3MTUyODQyNDgsImlzcyI6IjlkNzBlOTQ5NWQiLCJsYWJlbCI6IlVWRSIsImV4cCI6MTgwOTgzNTIwMSwiaWF0IjoxNzE1Mjg0MjQ4LCJqdGkiOiJmYWIxMTY1OC0zNzNmLTRlY2UtYTQwYy02YWE4NDJlNTU3MTQifQ.c0ZBw51ltLd0qGzzph1-kD3-dYT_PaY1Ge3egkqi_1g",
  siteId: '59bb8831-6706-4589-9ca0-ff74016e02b2',
  requestOptions: {
      // In production you might want to deal with this differently
      // cache: 'no-cache'
  }
}

export const appConfig: ApplicationConfig = {
  providers: [provideRouter(routes), dotcmsClientProvider(DOTCMS_CLIENT_CONFIG)]
};
