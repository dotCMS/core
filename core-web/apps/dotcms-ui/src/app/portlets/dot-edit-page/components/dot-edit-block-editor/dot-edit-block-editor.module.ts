import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgxTiptapModule } from '@dotcms/block-editor';
import { CommonModule } from '@angular/common';
import { DotEditBlockEditorComponent } from '@portlets/dot-edit-page/components/dot-edit-block-editor/dot-edit-block-editor.component';

@NgModule({
    declarations: [DotEditBlockEditorComponent],
    exports: [DotEditBlockEditorComponent],
    imports: [FormsModule, NgxTiptapModule, CommonModule]
})
export class DotEditBlockEditorModule {}
