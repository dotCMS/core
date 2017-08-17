import { NgModule } from '@angular/core';
import { CommonModule, LocationStrategy, HashLocationStrategy } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { DotBrowserComponent } from './dot-browser-component';

import {AutoCompleteModule} from 'primeng/primeng';
import {
  DotBreadcrumbModule, DotFileModule, DotFolderModule, DotHttpModule, DotNotificationModule, DotSettingsStorageModule,
  DotSiteBrowserModule, DotSiteDatatableModule, DotSiteSelectorModule, DotSiteTreeTableModule, DotTreeableDetailModule
} from 'dotcms-js/dotcms-js';


@NgModule({
    declarations: [DotBrowserComponent],
    imports: [
        CommonModule,
        DotBreadcrumbModule,
        DotSiteDatatableModule,
        DotSiteTreeTableModule,
        DotTreeableDetailModule,
        DotSiteSelectorModule,
        DotHttpModule,
        DotSettingsStorageModule,
        DotNotificationModule,
        DotFileModule,
        DotSiteBrowserModule,
        DotFolderModule,
        AutoCompleteModule,
        FormsModule,
        ReactiveFormsModule
    ],
    exports: [],
    providers: [
        { provide: LocationStrategy, useClass: HashLocationStrategy }
    ]
})
export class DotBrowserModule {}
