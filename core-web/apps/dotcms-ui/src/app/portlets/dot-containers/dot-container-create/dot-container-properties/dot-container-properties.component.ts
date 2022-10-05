import { Component, OnInit } from '@angular/core';
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

    constructor(private store: DotContainerPropertiesStore, private fb: UntypedFormBuilder) {
        //
    }

    ngOnInit(): void {
        this.form = this.fb.group({
            body: '',
            preLoop: '',
            postLoop: '',
            containerStructures: this.fb.array([])
        });
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
     * This method shows the Content type inputs
     */
    showContentTypeAndCode(): void {
        this.store.updateContentTypeVisibilty(true);
    }
}
