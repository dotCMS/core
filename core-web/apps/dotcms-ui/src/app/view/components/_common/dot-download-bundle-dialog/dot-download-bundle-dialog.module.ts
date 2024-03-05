import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { DropdownModule } from 'primeng/dropdown';
import { SelectButtonModule } from 'primeng/selectbutton';

import { DotPushPublishFiltersService } from '@dotcms/data-access';
import { DotFieldRequiredDirective, DotMessagePipe } from '@dotcms/ui';

import { DotDownloadBundleDialogComponent } from './dot-download-bundle-dialog.component';

import { DotDownloadBundleDialogService } from '../../../../api/services/dot-download-bundle-dialog/dot-download-bundle-dialog.service';
import { DotPipesModule } from '../../../pipes/dot-pipes.module';
import { DotDialogModule } from '../../dot-dialog/dot-dialog.module';

@NgModule({
    declarations: [DotDownloadBundleDialogComponent],
    exports: [DotDownloadBundleDialogComponent],
    providers: [DotPushPublishFiltersService, DotDownloadBundleDialogService],
    imports: [
        CommonModule,
        FormsModule,
        DotDialogModule,
        ReactiveFormsModule,
        DropdownModule,
        SelectButtonModule,
        DotPipesModule,
        DotFieldRequiredDirective,
        DotMessagePipe
    ]
})
export class DotDownloadBundleDialogModule {}
