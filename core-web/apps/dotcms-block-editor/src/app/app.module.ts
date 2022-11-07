import { NgModule, Injector } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

import { OrderListModule } from 'primeng/orderlist';
import { ListboxModule } from 'primeng/listbox';

import { createCustomElement } from '@angular/elements';
import { AppComponent } from './app.component';
import { DotBlockEditorComponent, BlockEditorModule } from '@dotcms/block-editor';
import { HttpClientModule } from '@angular/common/http';

@NgModule({
    declarations: [AppComponent],
    imports: [
        BrowserModule,
        BrowserAnimationsModule,
        CommonModule,
        FormsModule,
        BlockEditorModule,
        OrderListModule,
        ListboxModule,
        HttpClientModule
    ],
    providers: []
})
export class AppModule {
    constructor(private injector: Injector) {}

    ngDoBootstrap() {
        if (customElements.get('dotcms-block-editor') === undefined) {
            const element = createCustomElement(DotBlockEditorComponent, {
                injector: this.injector
            });
            customElements.define('dotcms-block-editor', element);
        }
    }
}
