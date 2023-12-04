import { CommonModule } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';
import { DoBootstrap, Injector, NgModule } from '@angular/core';
import { createCustomElement } from '@angular/elements';
import { FormsModule } from '@angular/forms';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { ListboxModule } from 'primeng/listbox';
import { OrderListModule } from 'primeng/orderlist';

import { BlockEditorModule, DotBlockEditorComponent } from '@dotcms/block-editor';
import { DotPropertiesService } from '@dotcms/data-access';

import { AppComponent } from './app.component';

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
    providers: [DotPropertiesService]
})
export class AppModule implements DoBootstrap {
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
