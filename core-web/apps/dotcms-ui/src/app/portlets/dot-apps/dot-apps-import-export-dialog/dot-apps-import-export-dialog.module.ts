import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';

import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';

import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';
import { DotAppsService } from '@dotcms/app/api/services/dot-apps/dot-apps.service';
import { DotAutofocusDirective, DotFieldRequiredDirective, DotMessagePipe } from '@dotcms/ui';
import { DotPipesModule } from '@pipes/dot-pipes.module';

import { DotAppsImportExportDialogComponent } from './dot-apps-import-export-dialog.component';

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
