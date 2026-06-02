import { mergeApplicationConfig, ApplicationConfig } from '@angular/core';
import { provideServerRendering, withRoutes } from '@angular/ssr';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { appConfig } from './app.config';
import { serverRoutes } from './app.routes.server';
import { serverBaseUrlInterceptor } from './server-base-url.interceptor';

const serverConfig: ApplicationConfig = {
  providers: [
    provideServerRendering(withRoutes(serverRoutes)),
    // Re-provide HttpClient on the server with an interceptor that turns the
    // app's relative API calls into in-process localhost requests, so SSR does
    // not round-trip through the public (proxy) host. See the interceptor.
    provideHttpClient(withFetch(), withInterceptors([serverBaseUrlInterceptor]))
  ]
};

export const config = mergeApplicationConfig(appConfig, serverConfig);
