import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { InputTextModule } from 'primeng/inputtext';

import { DotAppsListComponent } from './dot-apps-list.component';
import { DotAppsCardModule } from './dot-apps-card/dot-apps-card.module';
import { DotAppsService } from '@services/dot-apps/dot-apps.service';
import { DotAppsListResolver } from './dot-apps-list-resolver.service';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { NotLicensedModule } from '@components/not-licensed/not-licensed.module';
import { ButtonModule } from 'primeng/button';
import { DotAppsImportExportDialogModule } from '../dot-apps-import-export-dialog/dot-apps-import-export-dialog.module';
import { DotIconModule } from '@dotcms/ui';
import { DotPortletBaseModule } from '@components/dot-portlet-base/dot-portlet-base.module';

@NgModule({
    imports: [
        InputTextModule,
        CommonModule,
        ButtonModule,
        DotAppsCardModule,
        DotPipesModule,
        DotAppsImportExportDialogModule,
        NotLicensedModule,
        DotIconModule,
        DotPortletBaseModule
    ],
    declarations: [DotAppsListComponent],
    exports: [DotAppsListComponent],
    providers: [DotAppsService, DotAppsListResolver]
})
export class DotAppsListModule {}
