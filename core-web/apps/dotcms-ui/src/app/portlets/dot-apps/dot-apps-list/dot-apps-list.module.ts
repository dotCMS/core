import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';

import { DotPortletBaseModule } from '@components/dot-portlet-base/dot-portlet-base.module';
import { DotAppsService } from '@dotcms/app/api/services/dot-apps/dot-apps.service';
import { DotIconModule, DotMessagePipe, DotNotLicenseComponent, DotSafeHtmlPipe } from '@dotcms/ui';

import { DotAppsCardModule } from './dot-apps-card/dot-apps-card.module';
import { DotAppsListResolver } from './dot-apps-list-resolver.service';
import { DotAppsListComponent } from './dot-apps-list.component';

import { DotAppsImportExportDialogModule } from '../dot-apps-import-export-dialog/dot-apps-import-export-dialog.module';

@NgModule({
    imports: [
        InputTextModule,
        CommonModule,
        ButtonModule,
        DotAppsCardModule,
        DotSafeHtmlPipe,
        DotAppsImportExportDialogModule,
        DotNotLicenseComponent,
        DotIconModule,
        DotPortletBaseModule,
        DotMessagePipe
    ],
    declarations: [DotAppsListComponent],
    exports: [DotAppsListComponent],
    providers: [DotAppsService, DotAppsListResolver]
})
export class DotAppsListModule {}
