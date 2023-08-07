import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { DropdownModule } from 'primeng/dropdown';
import { SelectButtonModule } from 'primeng/selectbutton';
import { TableModule } from 'primeng/table';

import { DotContentCompareTableComponent } from '@components/dot-content-compare/components/dot-content-compare-table/dot-content-compare-table.component';
import { DotContentComparePreviewFieldComponent } from '@components/dot-content-compare/components/fields/dot-content-compare-preview-field/dot-content-compare-preview-field.component';
import { DotContentCompareComponent } from '@components/dot-content-compare/dot-content-compare.component';
import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';
import { BlockEditorModule } from '@dotcms/block-editor';
import { DotContentletService, DotVersionableService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';
import { DotDiffPipeModule } from '@pipes/dot-diff/dot-diff.pipe.module';

import { DotContentCompareBlockEditorComponent } from './components/dot-content-compare-block-editor/dot-content-compare-block-editor.component';
import { DotContentCompareDialogComponent } from './components/dot-content-compare-dialog/dot-content-compare-dialog.component';
import { DotTransformVersionLabelPipe } from './pipes/dot-transform-version-label.pipe';

import { DotPipesModule } from '../../pipes/dot-pipes.module';

@NgModule({
    declarations: [
        DotContentCompareComponent,
        DotContentCompareTableComponent,
        DotContentCompareDialogComponent,
        DotContentCompareBlockEditorComponent,
        DotContentComparePreviewFieldComponent,
        DotTransformVersionLabelPipe
    ],
    exports: [DotContentCompareDialogComponent],
    imports: [
        CommonModule,
        DotDialogModule,
        TableModule,
        DropdownModule,
        SelectButtonModule,
        FormsModule,
        DotMessagePipe,
        DotDiffPipeModule,
        ButtonModule,
        BlockEditorModule,
        DotPipesModule
    ],
    providers: [DotContentletService, DotVersionableService]
})
export class DotContentCompareModule {}
