import { MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';
import { firstValueFrom, of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import {
    DoBootstrap,
    inject,
    Injector,
    NgModule,
    provideAppInitializer,
    Type
} from '@angular/core';
import { createCustomElement } from '@angular/elements';
import { BrowserModule } from '@angular/platform-browser';
import { provideAnimations } from '@angular/platform-browser/animations';

import { catchError, switchMap, take } from 'rxjs/operators';

import {
    DotLocalstorageService,
    DotMessageService,
    DotUploadService,
    DotWorkflowActionsFireService
} from '@dotcms/data-access';
import { DotcmsConfigService, LoggerService, StringUtils } from '@dotcms/dotcms-js';
import { DotBinaryFieldCeBridgeComponent } from '@dotcms/edit-content';
import { provideDotCMSTheme } from '@dotcms/ui';

import { AppComponent } from './app.component';

interface ContenttypeFieldElement {
    tag: string;
    component: Type<DotBinaryFieldCeBridgeComponent>;
}

const CONTENTTYPE_FIELDS: ContenttypeFieldElement[] = [
    {
        tag: 'dotcms-binary-field',
        component: DotBinaryFieldCeBridgeComponent
    }
];

@NgModule({
    declarations: [AppComponent],
    imports: [BrowserModule, DotBinaryFieldCeBridgeComponent, MonacoEditorModule],
    providers: [
        provideHttpClient(),
        provideAnimations(),
        DotMessageService,
        LoggerService,
        StringUtils,
        DotUploadService,
        DotWorkflowActionsFireService,
        provideDotCMSTheme(),
        provideAppInitializer(() => {
            const messageService = inject(DotMessageService);
            const configService = inject(DotcmsConfigService);
            const storage = inject(DotLocalstorageService);
            const logger = inject(LoggerService);
            let language = 'default';
            try {
                const cachedLanguage = storage.getItem<string>('dotMessagesKeys-lang');
                if (
                    typeof cachedLanguage === 'string' &&
                    /^[a-zA-Z][a-zA-Z0-9_-]*$/.test(cachedLanguage)
                ) {
                    language = cachedLanguage;
                }
            } catch (error) {
                logger.warn('Unable to read cached translation locale; using default.', error);
            }

            return firstValueFrom(
                configService.getConfig().pipe(
                    take(1),
                    catchError((error) => {
                        logger.warn(
                            'Unable to load configuration; fetching uncached translations.',
                            error
                        );

                        return of(null);
                    }),
                    switchMap((config) =>
                        messageService.init({
                            language,
                            languageBundleVersion: config?.languageBundleVersion
                        })
                    )
                )
            );
        })
    ]
})
export class AppModule implements DoBootstrap {
    constructor(private readonly injector: Injector) {}

    ngDoBootstrap(): void {
        try {
            CONTENTTYPE_FIELDS.forEach(({ tag, component }) => {
                // prevent 'has already been defined as a custom element' error
                if (customElements.get(tag)) {
                    return;
                }

                // create custom elements from angular components
                const el = createCustomElement(component, {
                    injector: this.injector
                });

                // define in browser registry
                customElements.define(tag, el);
            });
        } catch (err) {
            console.error(err);
        }
    }
}
