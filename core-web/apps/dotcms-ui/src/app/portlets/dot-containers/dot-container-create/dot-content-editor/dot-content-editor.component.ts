import { Component, Input } from '@angular/core';
import { DotContentEditorStore } from '@portlets/dot-containers/dot-container-create/dot-content-editor/store/dot-content-editor.store';
import { UntypedFormGroup } from '@angular/forms';
import { DialogService } from 'primeng/dynamicdialog';
import { DotAddVariableComponent } from './dot-add-variable/dot-add-variable.component';
import { DotMessageService } from '@dotcms/app/api/services/dot-message/dot-messages.service';

@Component({
    selector: 'dot-content-editor',
    templateUrl: './dot-content-editor.component.html',
    styleUrls: ['./dot-content-editor.component.scss'],
    providers: [DotContentEditorStore]
})
export class DotContentEditorComponent {
    @Input() form: UntypedFormGroup;
    vm$ = this.store.vm$;

    constructor(
        private store: DotContentEditorStore,
        private dialogService: DialogService,
        private dotMessageService: DotMessageService
    ) {
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
        } else {
            this.store.updateActiveTabIndex(index);
        }

        return false;
    }

    handleClose(index: number = null, close: () => void): void {
        this.store.updateClosedTab(index - 1);
        close();
    }

    handleAddVariable() {
        this.dialogService.open(DotAddVariableComponent, {
            header: this.dotMessageService.get('containers.properties.add.variable.title'),
            width: '50rem'
            // data: {
            //     onSave: () => {}
            // }
        });
    }
}
