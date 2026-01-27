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
import {
    DotPropertiesService,
    DotContentSearchService,
    DotMessageService
} from '@dotcms/data-access';
import { DotAssetSearchComponent, provideDotCMSTheme } from '@dotcms/ui';

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
        HttpClientModule,
        DotAssetSearchComponent
    ],
    providers: [
        DotPropertiesService,
        DotContentSearchService,
        DotMessageService,
        provideDotCMSTheme()
    ]
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
