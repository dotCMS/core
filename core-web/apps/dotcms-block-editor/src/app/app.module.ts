import { NgModule, Injector } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

import { createCustomElement } from '@angular/elements';
import { AppComponent } from './app.component';
import { DotBlockEditorComponent, NgxTiptapModule } from '@dotcms/block-editor';

@NgModule({
    declarations: [AppComponent],
    imports: [BrowserModule, BrowserAnimationsModule, CommonModule, FormsModule, NgxTiptapModule],
    providers: []
})
export class AppModule {
    constructor(private injector: Injector) {}

    ngDoBootstrap() {
        const element = createCustomElement(DotBlockEditorComponent, {
            injector: this.injector
        });

        customElements.define('dotcms-block-editor', element);
    }
}
