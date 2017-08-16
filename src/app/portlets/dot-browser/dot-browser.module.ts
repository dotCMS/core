import { NgModule } from '@angular/core';
import { CommonModule, LocationStrategy, HashLocationStrategy } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { DotBrowserComponent } from './dot-browser-component';
import {
    // COMPONENTS
    SiteSelectorComponent,
    DotcmsBreadcrumbModule,
    DotcmsSiteTreeTableModule,
    DotcmsSiteDatatableModule,
    DotcmsTreeableDetailModule,
    // SERVICES
    AppConfig,
    FileService,
    HttpClient,
    LocalStoreService,
    LoggerService,
    NotificationService,
    SettingsStorageService,
    SiteBrowserService,
    SiteBrowserState,
    SiteSelectorService,
    SiteTreetableService
} from '../../../dotcms-js';

import {AutoCompleteModule} from 'primeng/primeng';


@NgModule({
    declarations: [DotBrowserComponent, SiteSelectorComponent],
    imports: [
        CommonModule,
        DotcmsBreadcrumbModule,
        DotcmsSiteDatatableModule,
        DotcmsSiteTreeTableModule,
        DotcmsTreeableDetailModule,
        DotcmsTreeableDetailModule,
        AutoCompleteModule,
        FormsModule,
        ReactiveFormsModule
    ],
    exports: [],
    providers: [
        { provide: LocationStrategy, useClass: HashLocationStrategy },
        { provide: SettingsStorageService, useClass: SettingsStorageService },
        { provide: HttpClient, useClass: HttpClient },
        { provide: SiteSelectorService, useClass: SiteSelectorService },
        { provide: SiteBrowserService, useClass: SiteBrowserService },
        { provide: NotificationService, useClass: NotificationService },
        { provide: AppConfig, useValue: AppConfig },
        { provide: SiteBrowserState, useClass: SiteBrowserState },
        { provide: SiteTreetableService, useClass: SiteTreetableService },
        { provide: LoggerService, useClass: LoggerService },
        { provide: LocalStoreService, useClass: LocalStoreService },
        { provide: FileService, useClass: FileService },
    ]
})
export class DotBrowserModule {}
