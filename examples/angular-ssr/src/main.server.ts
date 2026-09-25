import { provideZoneChangeDetection } from "@angular/core";
import { BootstrapContext, bootstrapApplication } from '@angular/platform-browser';
import { App } from './app/app';
import { config } from './app/app.config.server';

const bootstrap = (context: BootstrapContext) =>
    bootstrapApplication(App, {...config, providers: [provideZoneChangeDetection(), ...config.providers]}, context);

export default bootstrap;
