import { ApplicationConfig } from '@angular/core';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { provideDotCMSClient } from './lib/dotcms-client-token';

const DOTCMS_CLIENT_CONFIG = {
  dotcmsUrl: "http://localhost:8080",
  authToken: "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhcGkwMTMxZTkyMS1iYjBjLTQxMDItOGE2ZS1lM2IwZTY1YTg1OGUiLCJ4bW9kIjoxNzE1NjIzMzk1MDAwLCJuYmYiOjE3MTU2MjMzOTUsImlzcyI6ImRkYjQ4YzdiOTUiLCJsYWJlbCI6IkVNQSIsImV4cCI6MTgxMDE4MDgwMCwiaWF0IjoxNzE1NjIzMzk1LCJqdGkiOiIxZGRlMGE1Ni05MmUzLTQzMzktYmQ4Zi0zOTFlZGRmZWQ5MzEifQ.0XGTkOueg4lxz55m04RLPoF565ZjCjq-zx2ToOWP5YQ",
  siteId: '59bb8831-6706-4589-9ca0-ff74016e02b2',
  requestOptions: {
      // In production you might want to deal with this differently
      // cache: 'no-cache'
  }
}

export const appConfig: ApplicationConfig = {
  // TODO: provideDotCMSClient
  providers: [provideRouter(routes), provideDotCMSClient(DOTCMS_CLIENT_CONFIG)]
};
