import { NgModule } from '@angular/core';
import { DotLoopEditorComponent } from '@portlets/dot-containers/dot-container-create/dot-loop-editor/dot-loop-editor.component';
import { DotTextareaContentModule } from '@components/_common/dot-textarea-content/dot-textarea-content.module';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { CommonModule } from '@angular/common';
import { ButtonModule } from 'primeng/button';
import { ReactiveFormsModule } from '@angular/forms';

@NgModule({
    declarations: [DotLoopEditorComponent],
    imports: [
        DotTextareaContentModule,
        DotMessagePipeModule,
        CommonModule,
        ButtonModule,
        ReactiveFormsModule
    ],
    exports: [DotLoopEditorComponent]
})
export class DotLoopEditorModule {}
