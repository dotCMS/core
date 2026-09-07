import { MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';
import { firstValueFrom } from 'rxjs';

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

import {
    DotMessageService,
    DotUploadService,
    DotWorkflowActionsFireService
} from '@dotcms/data-access';
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
        DotUploadService,
        DotWorkflowActionsFireService,
        provideDotCMSTheme(),
        provideAppInitializer(() => {
            const dotMessageService = inject(DotMessageService);

            return firstValueFrom(dotMessageService.init());
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
