import { MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';
import { MarkdownModule } from 'ngx-markdown';

import { provideHttpClient } from '@angular/common/http';
import { ApplicationConfig, importProvidersFrom } from '@angular/core';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import {
    provideRouter,
    RouteReuseStrategy,
    withHashLocation,
    withRouterConfig
} from '@angular/router';

import { provideDotCMSTheme } from '@dotcms/ui';

import { appRoutes } from './app.routes';
import { NGFACES_MODULES } from './modules';
import { ENV_PROVIDERS } from './providers';
import { DotCustomReuseStrategyService } from './shared/dot-custom-reuse-strategy/dot-custom-reuse-strategy.service';
import { DotDirectivesModule } from './shared/dot-directives.module';
import { SharedModule } from './shared/shared.module';
import { DotLoginPageResolver } from './view/components/login/dot-login-page-resolver.service';

export const appConfig: ApplicationConfig = {
    providers: [
        // Core Angular providers
        provideAnimationsAsync(),
        provideDotCMSTheme(),
        provideAnimations(),
        provideHttpClient(),
        provideRouter(
            appRoutes,
            withHashLocation(),
            withRouterConfig({
                onSameUrlNavigation: 'reload'
            })
        ),

        // Router providers
        { provide: RouteReuseStrategy, useClass: DotCustomReuseStrategyService },
        DotLoginPageResolver,

        // Application providers
        ...ENV_PROVIDERS,

        // Module providers (using importProvidersFrom for modules that haven't been migrated yet)
        importProvidersFrom(
            // PrimeNG modules
            ...NGFACES_MODULES,
            // Third-party modules
            MonacoEditorModule,
            MarkdownModule.forRoot(),
            // Shared modules
            DotDirectivesModule,
            SharedModule.forRoot()
        )
    ]
};
