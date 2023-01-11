import { MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';

import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { SelectButtonModule } from 'primeng/selectbutton';

import { DotTextareaContentComponent } from './dot-textarea-content.component';

@NgModule({
    imports: [CommonModule, SelectButtonModule, FormsModule, MonacoEditorModule],
    declarations: [DotTextareaContentComponent],
    exports: [DotTextareaContentComponent]
})
export class DotTextareaContentModule {}
