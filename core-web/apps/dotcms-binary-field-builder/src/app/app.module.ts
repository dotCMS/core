import { MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';

import { HttpClientModule } from '@angular/common/http';
import { DoBootstrap, Injector, NgModule, Type } from '@angular/core';
import { createCustomElement } from '@angular/elements';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { DotMessageService, DotUploadService } from '@dotcms/data-access';
import { DotEditContentBinaryFieldComponent } from '@dotcms/edit-content';

import { AppComponent } from './app.component';

interface ContenttypeFieldElement {
    tag: string;
    component: Type<DotEditContentBinaryFieldComponent>;
}

const CONTENTTYPE_FIELDS: ContenttypeFieldElement[] = [
    {
        tag: 'dotcms-binary-field',
        component: DotEditContentBinaryFieldComponent
    }
];

@NgModule({
    declarations: [AppComponent],
    imports: [
        BrowserModule,
        BrowserAnimationsModule,
        HttpClientModule,
        DotEditContentBinaryFieldComponent,
        MonacoEditorModule
    ],
    providers: [DotMessageService, DotUploadService]
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
