import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { appRoutes } from './app.routes';
import { provideClientHydration, withEventReplay } from '@angular/platform-browser';

export const appConfig: ApplicationConfig = {
    providers: [
        provideClientHydration(withEventReplay()),
        provideZoneChangeDetection({ eventCoalescing: true }),
        provideRouter(appRoutes)
    ]
};
