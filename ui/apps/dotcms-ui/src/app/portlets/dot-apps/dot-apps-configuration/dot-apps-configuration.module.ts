import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { DotAppsConfigurationComponent } from './dot-apps-configuration.component';
import { DotAppsService } from '@services/dot-apps/dot-apps.service';
import { DotAppsConfigurationResolver } from './dot-apps-configuration-resolver.service';
import { DotAvatarModule } from '@components/_common/dot-avatar/dot-avatar.module';
import { DotActionButtonModule } from '@components/_common/dot-action-button/dot-action-button.module';
import { DotCopyButtonModule } from '@components/dot-copy-button/dot-copy-button.module';
import { DotAppsConfigurationListModule } from './dot-apps-configuration-list/dot-apps-configuration-list.module';
import { DotAppsConfigurationHeaderModule } from '../dot-apps-configuration-header/dot-apps-configuration-header.module';
import { DotPipesModule } from '@pipes/dot-pipes.module';

import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';
import { MarkdownModule } from 'ngx-markdown';
import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';
import { DotAppsImportExportDialogModule } from '../dot-apps-import-export-dialog/dot-apps-import-export-dialog.module';

@NgModule({
    imports: [
        InputTextModule,
        ButtonModule,
        CommonModule,
        DotAvatarModule,
        DotActionButtonModule,
        DotCopyButtonModule,
        DotAppsConfigurationHeaderModule,
        DotAppsConfigurationListModule,
        DotAppsImportExportDialogModule,
        DotDialogModule,
        DotPipesModule,
        MarkdownModule.forChild()
    ],
    declarations: [DotAppsConfigurationComponent],
    exports: [DotAppsConfigurationComponent],
    providers: [DotAppsService, DotAppsConfigurationResolver]
})
export class DotAppsConfigurationModule {}
