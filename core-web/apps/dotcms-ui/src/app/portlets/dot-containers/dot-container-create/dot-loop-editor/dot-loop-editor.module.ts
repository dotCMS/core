import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';

import { DotTextareaContentModule } from '@components/_common/dot-textarea-content/dot-textarea-content.module';
import { DotMessagePipe } from '@dotcms/ui';
import { DotLoopEditorComponent } from '@portlets/dot-containers/dot-container-create/dot-loop-editor/dot-loop-editor.component';

@NgModule({
    declarations: [DotLoopEditorComponent],
    imports: [
        DotTextareaContentModule,
        DotMessagePipe,
        CommonModule,
        ButtonModule,
        ReactiveFormsModule
    ],
    exports: [DotLoopEditorComponent]
})
export class DotLoopEditorModule {}
