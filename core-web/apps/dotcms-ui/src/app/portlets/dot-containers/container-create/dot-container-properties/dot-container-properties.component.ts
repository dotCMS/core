import { Component, Input, OnInit } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';

interface MonacoEditorOperation {
    range: number;
    text: string;
    forceMoveMarkers: boolean;
}

interface MonacoEditor {
    getSelection: () => number;
    executeEdits: (action: string, data: MonacoEditorOperation[]) => void;
}

@Component({
    selector: 'dot-container-properties',
    templateUrl: './dot-container-properties.component.html',
    styleUrls: ['./dot-container-properties.component.scss']
})
export class DotContainerPropertiesComponent implements OnInit {
    editor: MonacoEditor;
    form: UntypedFormGroup;

    @Input() body: string;

    constructor(private fb: UntypedFormBuilder) {
        //
    }

    ngOnInit(): void {
        this.form = this.fb.group({ body: this.body });
    }

    /**
     * This method initializes the monaco editor
     *
     * @param {*} editor
     * @memberof DotTemplateComponent
     */
    initEditor(editor: MonacoEditor): void {
        this.editor = editor;
    }
}
