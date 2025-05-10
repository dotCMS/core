import { HttpClientModule } from '@angular/common/http';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { RouterModule } from '@angular/router';

import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ChartModule } from 'primeng/chart';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { SkeletonModule } from 'primeng/skeleton';
import { TabViewModule } from 'primeng/tabview';

import { DotAlertConfirmModule } from '@components/_common/dot-alert-confirm/dot-alert-confirm.module';
import { DotPushPublishDialogModule } from '@components/_common/dot-push-publish-dialog';
import { DotLargeMessageDisplayModule } from '@components/dot-large-message-display/dot-large-message-display.module';
import { DotMessageDisplayModule } from '@components/dot-message-display/dot-message-display.module';
import {
    DotAlertConfirmService,
    DotHttpErrorManagerService,
    DotMessageDisplayService,
    DotMessageService,
    DotRouterService
} from '@dotcms/data-access';
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
import { DotIconModule, DotSpinnerModule } from '@dotcms/ui';

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
        DropdownModule,
        BrowserAnimationsModule,
        HttpClientModule,
        RouterModule.forRoot([]),
        TabViewModule,
        ChartModule,
        InputTextareaModule,
        ButtonModule,
        DotIconModule,
        FormsModule,
        SkeletonModule,
        DotSpinnerModule,
        ReactiveFormsModule,
        DotMessageDisplayModule,
        DotLargeMessageDisplayModule,
        DotPushPublishDialogModule,
        DotAlertConfirmModule
    ],
    providers: [
        DotHttpErrorManagerService,
        DotAlertConfirmService,
        DotMessageService,
        DotRouterService,
        ConfirmationService,
        DotMessageDisplayService,
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
