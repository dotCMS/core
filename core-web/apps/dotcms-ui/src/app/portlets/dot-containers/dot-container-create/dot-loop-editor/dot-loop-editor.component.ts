import { Component, EventEmitter, Input, Output } from '@angular/core';
import { MonacoEditor } from '@models/monaco-editor';

@Component({
    selector: 'dot-loop-editor',
    templateUrl: './dot-loop-editor.component.html',
    styleUrls: ['./dot-loop-editor.component.scss']
})
export class DotLoopEditorComponent {
    editor: MonacoEditor;

    @Input() isEditorVisible = false;
    @Output() addButtonClicked = new EventEmitter();

    constructor() {
        //
    }

    /**
     * This method initializes the monaco editor
     *
     * @param {MonacoEditor} editor
     * @memberof DotLoopEditorComponent
     */
    initEditor(editor: MonacoEditor): void {
        this.editor = editor;
    }

    /**
     * This method shows the Loop Editor Input
     *
     * @return void
     * @memberof DotLoopEditorComponent
     */
    handleClick(): void {
        this.addButtonClicked.emit();
    }
}
