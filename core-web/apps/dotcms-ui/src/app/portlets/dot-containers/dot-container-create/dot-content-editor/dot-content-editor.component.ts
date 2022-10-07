import { Component, EventEmitter, Input, Output } from '@angular/core';
import { DotContentEditorStore } from '@portlets/dot-containers/dot-container-create/dot-content-editor/store/dot-content-editor.store';
import { UntypedFormGroup } from '@angular/forms';
import { DialogService } from 'primeng/dynamicdialog';
import { DotAddVariableComponent } from './dot-add-variable/dot-add-variable.component';
import { DotMessageService } from '@dotcms/app/api/services/dot-message/dot-messages.service';
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

    updateContentTypeText(text: string) {
        this.store.updateSelectedContentTypeContent(text);
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
