import { MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';
import { MarkdownModule } from 'ngx-markdown';

import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { ApplicationConfig, importProvidersFrom } from '@angular/core';
import { provideAnimations } from '@angular/platform-browser/animations';
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
import { apiPrefixInterceptor } from './shared/interceptors/api-prefix.interceptor';
import { serverErrorInterceptor } from './shared/interceptors/server-error.interceptor';
import { SharedModule } from './shared/shared.module';
import { DotLoginPageResolver } from './view/components/login/dot-login-page-resolver.service';

export const appConfig: ApplicationConfig = {
    providers: [
        // Core Angular providers
        provideAnimations(),
        provideDotCMSTheme(),
        // Angular's default backend, which since v22 is `FetchBackend`.
        //
        // Upload progress is not available on it: fetch has no equivalent of `xhr.upload`, so
        // `HttpEventType.UploadProgress` never fires. `withXhr()` restores that and was tried here,
        // then dropped by developer decision — XHR sits too near the deprecation line, its
        // server-side half already being slated for removal. So a bulk upload reports activity
        // without a position, which the indicator renders as activity rather than as zero.
        provideHttpClient(withInterceptors([apiPrefixInterceptor, serverErrorInterceptor])),
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
