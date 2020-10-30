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
import { DotAppsExportDialogModule } from '../dot-apps-export-dialog/dot-apps-export-dialog.module';

@NgModule({
    imports: [
        InputTextModule,
        CommonModule,
        ButtonModule,
        DotAppsCardModule,
        DotPipesModule,
        DotAppsExportDialogModule,
        NotLicensedModule
    ],
    declarations: [DotAppsListComponent],
    exports: [DotAppsListComponent],
    providers: [DotAppsService, DotAppsListResolver]
})
export class DotAppsListModule {}
