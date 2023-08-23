import { MarkdownModule } from 'ngx-markdown';

import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';

import { DotActionButtonModule } from '@components/_common/dot-action-button/dot-action-button.module';
import { DotCopyButtonModule } from '@components/dot-copy-button/dot-copy-button.module';
import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';
import { DotAppsService } from '@dotcms/app/api/services/dot-apps/dot-apps.service';
import { DotMessagePipe } from '@dotcms/ui';
import { DotPipesModule } from '@pipes/dot-pipes.module';

import { DotAppsConfigurationListModule } from './dot-apps-configuration-list/dot-apps-configuration-list.module';
import { DotAppsConfigurationResolver } from './dot-apps-configuration-resolver.service';
import { DotAppsConfigurationComponent } from './dot-apps-configuration.component';

import { DotAppsConfigurationHeaderModule } from '../dot-apps-configuration-header/dot-apps-configuration-header.module';
import { DotAppsImportExportDialogModule } from '../dot-apps-import-export-dialog/dot-apps-import-export-dialog.module';

@NgModule({
    imports: [
        InputTextModule,
        ButtonModule,
        CommonModule,
        DotActionButtonModule,
        DotCopyButtonModule,
        DotAppsConfigurationHeaderModule,
        DotAppsConfigurationListModule,
        DotAppsImportExportDialogModule,
        DotDialogModule,
        DotPipesModule,
        MarkdownModule.forChild(),
        DotMessagePipe
    ],
    declarations: [DotAppsConfigurationComponent],
    exports: [DotAppsConfigurationComponent],
    providers: [DotAppsService, DotAppsConfigurationResolver]
})
export class DotAppsConfigurationModule {}
