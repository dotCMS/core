import { NgModule } from '@angular/core';
import { CommonModule, LocationStrategy, HashLocationStrategy } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { DotBrowserComponent } from './dot-browser-component';

import {AutoCompleteModule, DialogModule, FileUploadModule} from 'primeng/primeng';
import {
    DotBreadcrumbHostselectorModule,
    DotBreadcrumbModule,
    DotFileModule,
    DotFolderModule,
    DotHttpModule,
    DotNotificationModule,
    DotSettingsStorageModule,
    DotSiteBrowserModule, DotSiteDatagridModule,
    DotSiteDatatableModule,
    DotSiteSelectorModule,
    DotSiteTreeTableModule,
    DotTreeableDetailModule
} from 'dotcms-js/dotcms-js';
import { Routes, RouterModule } from '@angular/router';

const routes: Routes = [
    {
        component: DotBrowserComponent,
        path: ''
    }
];

@NgModule({
    declarations: [DotBrowserComponent],
    imports: [
        AutoCompleteModule,
        CommonModule,
        DotBreadcrumbModule,
        DotFileModule,
        DotFolderModule,
        DotHttpModule,
        DotNotificationModule,
        DotSettingsStorageModule,
        DotSiteBrowserModule,
        DotSiteDatatableModule,
        DotSiteDatagridModule,
        DotSiteSelectorModule,
        DotSiteTreeTableModule,
        DotTreeableDetailModule,
        DotBreadcrumbHostselectorModule,
        FormsModule,
        DialogModule,
        FileUploadModule,
        ReactiveFormsModule,
        RouterModule.forChild(routes)
    ],
    exports: [],
    providers: [{ provide: LocationStrategy, useClass: HashLocationStrategy }]
})
export class DotBrowserModule {}
