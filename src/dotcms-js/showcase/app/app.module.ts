import {BrowserModule}  from '@angular/platform-browser';
import {FormsModule} from "@angular/forms";
import {HttpModule, JsonpModule} from '@angular/http';
import {LocationStrategy, HashLocationStrategy} from "@angular/common";
import {Logger, LOG_LOGGER_PROVIDERS, Options as LoggerOptions, Level as LoggerLevel} from "angular2-logger/core";
import {NgModule} from '@angular/core';

import {AppComponent} from './app.component';
import {routing} from "./app.routing";

// DOTJS SERVICES
import {
    AppConfig,
    FileSystemService,
    HttpClient,
    JWTAuthService,
    LocalStoreService,
    LoggerService,
    NotificationService,
    SettingsStorageService,
    SiteBrowserService,
    SiteBrowserState,
    SiteSelectorService,
    SiteTreetableService,
} from "../../dotcms-js";

// DOTJS MODULES
import {
    DotcmsBreadcrumbModule,
    DotcmsSiteDatatableModule,
    DotcmsSiteSelectorModule,
    DotcmsSiteTreeTableModule,
    DotcmsTreeableDetailModule,
} from "../../dotcms-js";

// SHOWCASE PAGES
import {BreadcrumbDemoShowcase} from "../components/breadcrumb/breadcrumb";
import {IntroDemoShowcase} from "../components/intro/intro";
import {SiteDatatableDemoShowcase} from "../components/site-datatable/site-datatable";
import {SiteSelectorDemoShowcase} from "../components/site-selector/site-selector";
import {SiteTreeTableDemoShowcase} from "../components/site-treetable/site-treetable";
import {TreeableDetailComponentDemoShowcase} from "../components/treeable-detail/treeable-detail";
import {TieredMenuModule, CodeHighlighterModule} from 'primeng/primeng';

@NgModule({
    imports: [
        BrowserModule,
        FormsModule,
        HttpModule,
        JsonpModule,

        DotcmsBreadcrumbModule,
        DotcmsSiteDatatableModule,
        DotcmsSiteSelectorModule,
        DotcmsSiteTreeTableModule,
        DotcmsTreeableDetailModule,

        TieredMenuModule,
        CodeHighlighterModule,

        routing,
    ],
    declarations: [
        AppComponent,
        BreadcrumbDemoShowcase,
        IntroDemoShowcase,
        SiteDatatableDemoShowcase,
        SiteSelectorDemoShowcase,
        SiteTreeTableDemoShowcase,
        TreeableDetailComponentDemoShowcase
    ],
    providers: [
        Logger,
        {provide: HttpClient, useClass: HttpClient},
        {provide: AppConfig, useValue: AppConfig},
        {provide: NotificationService, useClass: NotificationService},
        {provide: FileSystemService, useClass: FileSystemService},
        {provide: SiteTreetableService, useClass: SiteTreetableService},
        {provide: SiteBrowserService, useClass: SiteBrowserService},
        {provide: SiteSelectorService, useClass: SiteSelectorService},
        {provide: LoggerOptions, useValue: { level: LoggerLevel.INFO } },
        {provide: LoggerService, useClass: LoggerService},
        {provide: JWTAuthService, useClass: JWTAuthService},
        {provide: SiteBrowserState, useClass: SiteBrowserState},
        {provide: SettingsStorageService, useClass: SettingsStorageService},
        {provide: LocalStoreService, useClass: LocalStoreService},
        {provide: LocationStrategy, useClass: HashLocationStrategy}
    ],
    bootstrap: [ AppComponent ]
})
export class AppModule { }