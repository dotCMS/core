import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';

import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';

import {
    DotAutofocusDirective,
    DotDialogComponent,
    DotFieldRequiredDirective,
    DotMessagePipe,
    DotSafeHtmlPipe
} from '@dotcms/ui';

import { DotAppsImportExportDialogComponent } from './dot-apps-import-export-dialog.component';

import { DotAppsService } from '../../../api/services/dot-apps/dot-apps.service';

@NgModule({
    imports: [
        InputTextModule,
        CommonModule,
        PasswordModule,
        DotDialogComponent,
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
