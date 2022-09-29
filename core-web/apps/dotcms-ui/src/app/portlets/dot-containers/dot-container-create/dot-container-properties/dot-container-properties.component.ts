import { Component, Input, OnInit } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { DotContainerPropertiesStore } from '@portlets/dot-containers/dot-container-create/dot-container-properties/store/dot-container-properties.store';
import { MonacoEditor } from '@models/monaco-editor';

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
     * @param {MonacoEditor} editor
     * @memberof DotContainerPropertiesComponent
     */
    initEditor(editor: MonacoEditor): void {
        this.editor = editor;
    }

    /**
     * This method shows the Pre- and Post-Loop Inputs
     *
     * @return void
     * @memberof DotContainerPropertiesComponent
     */
    showLoopInput(): void {
        this.store.updatePrePostLoopInputVisibility(true);
    }

    /**
     * Method to stop propogation of Tab click event
     *
     * @param e {MouseEvent}
     * @param index {number}
     * @return boolean
     * @memberof DotContainerPropertiesComponent
     */
    handleChange(e: MouseEvent, index: number = null): boolean {
        if (index === null) {
            e.preventDefault();
            e.stopPropagation();
        }

        return false;
    }
}
