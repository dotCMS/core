import { Component } from '@angular/core';
import { DotContentEditorStore } from '@portlets/dot-containers/dot-container-create/dot-content-editor/store/dot-content-editor.store';

@Component({
    selector: 'dot-content-editor',
    templateUrl: './dot-content-editor.component.html',
    styleUrls: ['./dot-content-editor.component.scss'],
    providers: [DotContentEditorStore]
})
export class DotContentEditorComponent {
    vm$ = this.store.vm$;

    constructor(private store: DotContentEditorStore) {
        //
    }

    /**
     * Method to stop propogation of Tab click event
     *
     * @param e {MouseEvent}
     * @param index {number}
     * @return boolean
     * @memberof DotContentEditorComponent
     */
    handleChange(e: MouseEvent, index: number = null): boolean {
        if (index === null) {
            e.preventDefault();
            e.stopPropagation();
        }

        return false;
    }
}
