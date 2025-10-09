import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { DropdownModule } from 'primeng/dropdown';
import { SelectButtonModule } from 'primeng/selectbutton';
import { TableModule } from 'primeng/table';

import { BlockEditorModule } from '@dotcms/block-editor';
import { DotContentletService, DotVersionableService } from '@dotcms/data-access';
import {
    DotDialogComponent,
    DotDiffPipe,
    DotMessagePipe,
    DotRelativeDatePipe,
    DotSafeHtmlPipe
} from '@dotcms/ui';

import { DotContentCompareBlockEditorComponent } from './components/dot-content-compare-block-editor/dot-content-compare-block-editor.component';
import { DotContentCompareDialogComponent } from './components/dot-content-compare-dialog/dot-content-compare-dialog.component';
import { DotContentCompareTableComponent } from './components/dot-content-compare-table/dot-content-compare-table.component';
import { DotContentComparePreviewFieldComponent } from './components/fields/dot-content-compare-preview-field/dot-content-compare-preview-field.component';
import { DotContentCompareComponent } from './dot-content-compare.component';

@NgModule({
    declarations: [
        DotContentCompareComponent,
        DotContentCompareTableComponent,
        DotContentCompareDialogComponent,
        DotContentCompareBlockEditorComponent,
        DotContentComparePreviewFieldComponent
    ],
    exports: [DotContentCompareDialogComponent, DotContentCompareComponent],
    imports: [
        CommonModule,
        DotDialogComponent,
        TableModule,
        DropdownModule,
        SelectButtonModule,
        FormsModule,
        DotMessagePipe,
        DotDiffPipe,
        ButtonModule,
        BlockEditorModule,
        DotSafeHtmlPipe,
        DotRelativeDatePipe
    ],
    providers: [DotContentletService, DotVersionableService]
})
export class DotContentCompareModule {}
