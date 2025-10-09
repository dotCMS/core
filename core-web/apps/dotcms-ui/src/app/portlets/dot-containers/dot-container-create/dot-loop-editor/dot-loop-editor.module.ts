import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';

import { DotMessagePipe } from '@dotcms/ui';

import { DotLoopEditorComponent } from './dot-loop-editor.component';

import { DotTextareaContentComponent } from '../../../../view/components/_common/dot-textarea-content/dot-textarea-content.component';

@NgModule({
    declarations: [DotLoopEditorComponent],
    imports: [
        DotTextareaContentComponent,
        DotMessagePipe,
        CommonModule,
        ButtonModule,
        ReactiveFormsModule
    ],
    exports: [DotLoopEditorComponent]
})
export class DotLoopEditorModule {}
