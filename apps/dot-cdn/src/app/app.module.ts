import { NgModule } from '@angular/core';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { BrowserModule } from '@angular/platform-browser';
import { InputTextModule } from 'primeng/inputtext';
import { DropdownModule } from 'primeng/dropdown';

import { AppComponent } from './app.component';
import { CoreWebService, LoggerService, StringUtils } from '@dotcms/dotcms-js';
import { RouterModule } from '@angular/router';
import { HttpClientModule } from '@angular/common/http';

import { TabViewModule } from 'primeng/tabview';
import { ChartModule } from 'primeng/chart';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { ButtonModule } from 'primeng/button';
import { DotIconModule } from '@dotcms/ui';

@NgModule({
    declarations: [AppComponent],
    imports: [
        BrowserModule,
        InputTextModule,
        DropdownModule,
        BrowserAnimationsModule,
        HttpClientModule,
        RouterModule.forRoot([]),
        TabViewModule,
        ChartModule,
        InputTextareaModule,
        ButtonModule,
        DotIconModule
    ],
    providers: [CoreWebService, LoggerService, StringUtils],
    bootstrap: [AppComponent]
})
export class AppModule {}
