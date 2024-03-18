import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';

import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';

import { DotAppsService } from '@dotcms/app/api/services/dot-apps/dot-apps.service';
import {
    DotAutofocusDirective,
    DotDialogModule,
    DotFieldRequiredDirective,
    DotMessagePipe,
    DotSafeHtmlPipe
} from '@dotcms/ui';

import { DotAppsImportExportDialogComponent } from './dot-apps-import-export-dialog.component';

@NgModule({
    imports: [
        InputTextModule,
        CommonModule,
        PasswordModule,
        DotDialogModule,
        DotAutofocusDirective,
        ReactiveFormsModule,
        DotSafeHtmlPipe,
        DotFieldRequiredDirective,
        DotMessagePipe
    ],
    declarations: [DotAppsImportExportDialogComponent],
    exports: [DotAppsImportExportDialogComponent],
    providers: [DotAppsService]
})
export class DotAppsImportExportDialogModule {}
