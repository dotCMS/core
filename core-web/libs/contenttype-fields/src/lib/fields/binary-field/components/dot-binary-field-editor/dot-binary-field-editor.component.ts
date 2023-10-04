import { MonacoEditorConstructionOptions, MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';

import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';

@Component({
    selector: 'dot-dot-binary-field-editor',
    standalone: true,
    imports: [CommonModule, MonacoEditorModule],
    templateUrl: './dot-binary-field-editor.component.html',
    styleUrls: ['./dot-binary-field-editor.component.scss']
})
export class DotBinaryFieldEditorComponent {
    editorOptions: MonacoEditorConstructionOptions = {
        theme: 'vs',
        minimap: {
            enabled: false
        },
        cursorBlinking: 'solid',
        overviewRulerBorder: false,
        mouseWheelZoom: false,
        lineNumbers: 'on',
        selectionHighlight: false,
        roundedSelection: false,
        selectOnLineNumbers: false,
        columnSelection: false,
        language: 'javascript'
    };
}
