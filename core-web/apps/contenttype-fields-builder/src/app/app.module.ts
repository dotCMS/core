import { DoBootstrap, Injector, NgModule, Type } from '@angular/core';
import { createCustomElement } from '@angular/elements';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { ContenttypeFieldsModule } from '@dotcms/contenttype-fields';

import { AppComponent } from './app.component';

interface ContenttypeFieldElement {
    tag: string;
    component: Type<unknown>; // Expected to be a component
}

const CONTENTTYPE_FIELDS: ContenttypeFieldElement[] = [];

@NgModule({
    declarations: [AppComponent],
    imports: [BrowserModule, BrowserAnimationsModule, ContenttypeFieldsModule],
    bootstrap: [AppComponent]
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
