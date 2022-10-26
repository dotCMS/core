import {
    Component,
    EventEmitter,
    Input,
    OnChanges,
    OnInit,
    Output,
    SimpleChange
} from '@angular/core';
import { DotContentEditorStore } from '@portlets/dot-containers/dot-container-create/dot-content-editor/store/dot-content-editor.store';
import { DialogService } from 'primeng/dynamicdialog';
import { DotAddVariableComponent } from './dot-add-variable/dot-add-variable.component';
import { DotMessageService } from '@dotcms/app/api/services/dot-message/dot-messages.service';
import { DotCMSContentType } from '@dotcms/dotcms-models';
import { MenuItem } from '@dotcms/dotcms-js';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { DotContainerStructure } from '@models/container/dot-container.model';

@Component({
    selector: 'dot-content-code-editor',
    templateUrl: './dot-content-editor.component.html',
    styleUrls: ['./dot-content-editor.component.scss'],
    providers: [DotContentEditorStore]
})
export class DotContentEditorComponent implements OnInit, OnChanges {
    @Input() containerStructures: DotContainerStructure[];
    @Output() updateContainerStructure = new EventEmitter<MenuItem[]>();

    inputContainerStructures: DotContainerStructure[];
    vm$ = this.store.vm$;
    contentTypesData$ = this.store.contentTypeData$;
    monacoEditors = {};
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

    ngOnInit(): void {
        this.store.contentTypes$.pipe(takeUntil(this.destroy$)).subscribe(() => {
            this.store.updateRetrievedContentTypes(this.containerStructures ?? []);
        });
    }

    ngOnChanges(changes: { [property: string]: SimpleChange }) {
        const change: SimpleChange = changes['containerStructures'];

        this.inputContainerStructures = change.currentValue;

        this.store.updateRetrievedContentTypes(this.inputContainerStructures ?? []);
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
     * It opens a dialog with a form to add a variable to the container
     * @param {DotCMSContentType} contentType - DotCMSContentType - The content type object that contains
     * the variables.
     * @returns {void}
     * @param {number} index - The index of the tab that was clicked.
     * @memberof DotContentEditorComponent
     */
    handleAddVariable(contentType: DotCMSContentType) {
        this.dialogService.open(DotAddVariableComponent, {
            header: this.dotMessageService.get('containers.properties.add.variable.title'),
            width: '50rem',
            data: {
                contentTypeVariable: contentType.variable,
                onSave: (variable) => {
                    const editor = this.monacoEditors[contentType.name].getModel();
                    this.monacoEditors[contentType.name]
                        .getModel()
                        .setValue(editor.getValue() + `${variable}`);
                }
            }
        });
    }

    /**
     * It pushes the monaco instance into the monacoEditor array.
     * @param monacoInstance - The monaco instance that is created by the component.
     */
    monacoInit(monacoEditor) {
        this.monacoEditors[monacoEditor.name] = monacoEditor.editor;
    }
}
