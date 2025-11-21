import { HttpClientModule } from '@angular/common/http';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { RouterModule } from '@angular/router';

import { ButtonModule } from 'primeng/button';
import { ChartModule } from 'primeng/chart';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { SkeletonModule } from 'primeng/skeleton';
import { TabsModule } from 'primeng/tabs';
import { TextareaModule } from 'primeng/textarea';

import {
    CoreWebService,
    DotcmsConfigService,
    DotcmsEventsService,
    DotEventsSocket,
    DotEventsSocketURL,
    LoggerService,
    LoginService,
    SiteService,
    StringUtils
} from '@dotcms/dotcms-js';
import { DotIconComponent, DotSpinnerComponent } from '@dotcms/ui';

import { AppComponent } from './app.component';
import { DotCDNStore } from './dotcdn.component.store';

const dotEventSocketURLFactory = () => {
    return new DotEventsSocketURL(
        `${window.location.hostname}:${window.location.port}/api/ws/v1/system/events`,
        window.location.protocol === 'https:'
    );
};

@NgModule({
    declarations: [AppComponent],
    imports: [
        BrowserModule,
        InputTextModule,
        SelectModule,
        BrowserAnimationsModule,
        HttpClientModule,
        RouterModule.forRoot([]),
        TabsModule,
        ChartModule,
        TextareaModule,
        ButtonModule,
        DotIconComponent,
        FormsModule,
        SkeletonModule,
        DotSpinnerComponent,
        ReactiveFormsModule
    ],
    providers: [
        CoreWebService,
        LoggerService,
        StringUtils,
        SiteService,
        LoginService,
        DotEventsSocket,
        DotcmsEventsService,
        { provide: DotEventsSocketURL, useFactory: dotEventSocketURLFactory },
        DotcmsConfigService,
        DotCDNStore
    ],
    bootstrap: [AppComponent]
})
export class AppModule {}
