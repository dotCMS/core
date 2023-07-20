import { DoBootstrap, Injector, NgModule } from '@angular/core';
import { createCustomElement } from '@angular/elements';
import { BrowserModule } from '@angular/platform-browser';

import { WebComponentsModule, InputFieldComponent } from '@dotcms/web-components';

const CUSTOM_ELEMENTS = [
    {
        tag: 'custom-input-field',
        component: InputFieldComponent
    }
];

@NgModule({
    declarations: [],
    imports: [BrowserModule, WebComponentsModule],
    providers: []
})
export class AppModule implements DoBootstrap {
    constructor(private readonly injector: Injector) {}

    ngDoBootstrap(): void {
        try {
            CUSTOM_ELEMENTS.forEach(({ tag, component }) => {
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
