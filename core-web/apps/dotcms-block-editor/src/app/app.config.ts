import { provideHttpClient } from '@angular/common/http';
import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';

export const appConfig: ApplicationConfig = {
    providers: [provideBrowserGlobalErrorListeners(), provideHttpClient()]
};
