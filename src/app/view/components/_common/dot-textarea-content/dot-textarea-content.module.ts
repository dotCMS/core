import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotTextareaContentComponent } from './dot-textarea-content.component';
import { SelectButtonModule } from 'primeng/primeng';
import { FormsModule } from '@angular/forms';
import { AceEditorModule } from 'ng2-ace-editor';
import { TinymceModule } from 'angular2-tinymce';

@NgModule({
    imports: [
        AceEditorModule,
        CommonModule,
        SelectButtonModule,
        FormsModule,
        TinymceModule.withConfig({
            menubar: false,
            resize: false,
            auto_focus: false
        })
    ],
    declarations: [DotTextareaContentComponent],
    exports: [DotTextareaContentComponent]
})
export class DotTextareaContentModule {}
