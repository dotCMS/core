import { Component, Input, OnInit } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { DotContainerPropertiesStore } from '@portlets/dot-containers/container-create/dot-container-properties/store/dot-container-properties.store';

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
    styleUrls: ['./dot-container-properties.component.scss'],
    providers: [DotContainerPropertiesStore]
})
export class DotContainerPropertiesComponent implements OnInit {
    vm$ = this.store.vm$;
    editor: MonacoEditor;
    form: UntypedFormGroup;

    @Input() body: string;

    constructor(private store: DotContainerPropertiesStore, private fb: UntypedFormBuilder) {
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

    showLoopInput(): void {
        this.store.updatePrePostLoopInputVisibility(true);
    }

    handleChange(e): boolean {
        if (typeof e.index === 'undefined') {
            e.preventDefault();
            e.stopPropagation();
        }

        return false;
    }
}
