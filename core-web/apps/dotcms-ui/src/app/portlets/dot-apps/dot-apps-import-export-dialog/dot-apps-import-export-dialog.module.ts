import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';

import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';

import { DotAutofocusDirective, DotFieldRequiredDirective, DotMessagePipe } from '@dotcms/ui';

import { DotAppsImportExportDialogComponent } from './dot-apps-import-export-dialog.component';

import { DotAppsService } from '../../../api/services/dot-apps/dot-apps.service';
import { DotDialogModule } from '../../../view/components/dot-dialog/dot-dialog.module';
import { DotPipesModule } from '../../../view/pipes/dot-pipes.module';

@NgModule({
    imports: [
        InputTextModule,
        CommonModule,
        PasswordModule,
        DotDialogModule,
        DotAutofocusDirective,
        ReactiveFormsModule,
        DotPipesModule,
        DotFieldRequiredDirective,
        DotMessagePipe
    ],
    declarations: [DotAppsImportExportDialogComponent],
    exports: [DotAppsImportExportDialogComponent],
    providers: [DotAppsService]
})
export class DotAppsImportExportDialogModule {}
