import { MarkdownModule } from 'ngx-markdown';

import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';

import {
    DotCopyButtonComponent,
    DotDialogComponent,
    DotMessagePipe,
    DotSafeHtmlPipe
} from '@dotcms/ui';

import { DotAppsConfigurationListModule } from './dot-apps-configuration-list/dot-apps-configuration-list.module';
import { DotAppsConfigurationResolver } from './dot-apps-configuration-resolver.service';
import { DotAppsConfigurationComponent } from './dot-apps-configuration.component';

import { DotAppsService } from '../../../api/services/dot-apps/dot-apps.service';
import { DotActionButtonComponent } from '../../../view/components/_common/dot-action-button/dot-action-button.component';
import { DotAppsConfigurationHeaderModule } from '../dot-apps-configuration-header/dot-apps-configuration-header.module';
import { DotAppsImportExportDialogModule } from '../dot-apps-import-export-dialog/dot-apps-import-export-dialog.module';

@NgModule({
    imports: [
        InputTextModule,
        ButtonModule,
        CommonModule,
        DotActionButtonComponent,
        DotCopyButtonComponent,
        DotAppsConfigurationHeaderModule,
        DotAppsConfigurationListModule,
        DotAppsImportExportDialogModule,
        DotDialogComponent,
        DotSafeHtmlPipe,
        MarkdownModule.forChild(),
        DotMessagePipe
    ],
    declarations: [DotAppsConfigurationComponent],
    exports: [DotAppsConfigurationComponent],
    providers: [DotAppsService, DotAppsConfigurationResolver]
})
export class DotAppsConfigurationModule {}
