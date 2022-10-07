import { Component, EventEmitter, Input, Output } from '@angular/core';
import { DotContentEditorStore } from '@portlets/dot-containers/dot-container-create/dot-content-editor/store/dot-content-editor.store';
import { UntypedFormGroup } from '@angular/forms';
import { DialogService } from 'primeng/dynamicdialog';
import { DotAddVariableComponent } from './dot-add-variable/dot-add-variable.component';
import { DotMessageService } from '@dotcms/app/api/services/dot-message/dot-messages.service';
import { DotCMSContentType } from '@dotcms/dotcms-models';
import { MenuItem } from '@dotcms/dotcms-js';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Component({
    selector: 'dot-content-editor',
    templateUrl: './dot-content-editor.component.html',
    styleUrls: ['./dot-content-editor.component.scss'],
    providers: [DotContentEditorStore]
})
export class DotContentEditorComponent {
    @Input() form: UntypedFormGroup;
    @Output() updateContainerStructure = new EventEmitter<MenuItem[]>();
    vm$ = this.store.vm$;
    contentTypesData$ = this.store.contentTypeData$;

    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        private store: DotContentEditorStore,
        private dialogService: DialogService,
        private dotMessageService: DotMessageService
    ) {
        // send date to main component
        this.contentTypesData$
            .pipe(takeUntil(this.destroy$))
            .subscribe((contentTypesData: MenuItem[]) => {
                this.updateContainerStructure.emit(contentTypesData);
            });
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

    /**
     * It updates the content type text
     * @param {string} text - The text to be updated
     * @returns {void}
     * @memberof DotContentEditorComponent
     */
    updateContentTypeText(text: string): void {
        this.store.updateSelectedContentTypeContent(text);
    }

    /**
     * The function takes in an index and a close function. It then updates the store with the index of the tab that was
     * closed and then calls the close function
     * @param {number} [index=null] - The index of the tab that is being closed.
     * @param close - This is a function that closes the tab.
     * @returns {void}
     * @memberof DotContentEditorComponent
     */
    handleClose(index: number = null, close: () => void): void {
        this.store.updateClosedTab(index - 1);
        close();
    }

    /**
     * This function opens a dialog window that allows the user to add a variable to a content type
     * @param {DotCMSContentType} contentType - DotCMSContentType
     * @returns {void}
     * @memberof DotContentEditorComponent
     */
    handleAddVariable(contentType: DotCMSContentType) {
        this.dialogService.open(DotAddVariableComponent, {
            header: this.dotMessageService.get('containers.properties.add.variable.title'),
            width: '50rem',
            data: {
                contentTypeVariable: contentType.variable,
                onSave: () =>
                    //variable: string
                    {
                        //
                    }
            }
        });
    }
}
