import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { DotAppsService } from '@services/dot-apps/dot-apps.service';
import { DotPipesModule } from '@pipes/dot-pipes.module';

import { InputTextModule } from 'primeng/inputtext';
import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';
import { DotAutofocusModule } from 'projects/dot-rules/src/lib/directives/dot-autofocus/dot-autofocus.module';
import { ReactiveFormsModule } from '@angular/forms';
import { DotAppsImportExportDialogComponent } from './dot-apps-import-export-dialog.component';
import { PasswordModule } from 'primeng/password';

@NgModule({
    imports: [
        InputTextModule,
        CommonModule,
        PasswordModule,
        DotDialogModule,
        DotAutofocusModule,
        ReactiveFormsModule,
        DotPipesModule
    ],
    declarations: [DotAppsImportExportDialogComponent],
    exports: [DotAppsImportExportDialogComponent],
    providers: [DotAppsService]
})
export class DotAppsImportExportDialogModule {}
