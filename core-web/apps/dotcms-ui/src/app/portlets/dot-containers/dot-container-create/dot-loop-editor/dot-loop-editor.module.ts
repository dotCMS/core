import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';

import { DotMessagePipe } from '@dotcms/ui';

import { DotLoopEditorComponent } from './dot-loop-editor.component';

import { DotTextareaContentModule } from '../../../../view/components/_common/dot-textarea-content/dot-textarea-content.module';

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
