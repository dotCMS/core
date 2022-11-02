import { Component, EventEmitter, forwardRef, Input, OnInit, Output } from '@angular/core';
import { DialogService } from 'primeng/dynamicdialog';
import { DotAddVariableComponent } from './dot-add-variable/dot-add-variable.component';
import { DotMessageService } from '@dotcms/app/api/services/dot-message/dot-messages.service';
import { DotCMSContentType } from '@dotcms/dotcms-models';
import { Subject } from 'rxjs';
import { ControlValueAccessor, FormArray, FormControl, NG_VALUE_ACCESSOR } from '@angular/forms';
import { MenuItem } from 'primeng/api';

interface DotContainerContent extends DotCMSContentType {
    code?: string;
}

@Component({
    selector: 'dot-container-code',
    templateUrl: './dot-container-code.component.html',
    styleUrls: ['./dot-container-code.component.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DotContentEditorComponent),
            multi: true
        }
    ]
})
export class DotContentEditorComponent implements ControlValueAccessor, OnInit {
    @Input() contentTypes: DotContainerContent[];
    @Output() valueChange = new EventEmitter<MenuItem[]>();

    public readonly containerContents = new FormArray([] as FormControl<DotContainerContent>[]);

    menuItems: MenuItem[];
    activeTabIndex = 1;
    monacoEditors = {};

    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        private dialogService: DialogService,
        private dotMessageService: DotMessageService
    ) {}

    ngOnInit() {
        this.containerContents.valueChanges.subscribe((fieldVal) => {
            this._onChange(fieldVal);
            this.onTouched();
        });

        this.init();
    }

    public writeValue(value: DotContainerContent[] | null): void {
        value = value ?? [];
        this.containerContents.setValue(value);
    }

    public setDisabledState(isDisabled: boolean): void {
        if (isDisabled) {
            this.containerContents.disable();
        } else {
            this.containerContents.enable();
        }
    }

    private _onChange = (_value: DotContainerContent[] | null) => undefined;

    public registerOnChange(fn: (value: DotContainerContent[] | null) => void): void {
        this._onChange = fn;
    }

    public onTouched = () => undefined;

    public registerOnTouched(fn: () => void): void {
        this.onTouched = fn;
    }

    /**
     * If the index is null or 0, prevent the default action and stop propagation. Otherwise, update the active tab index
     * and push the container content
     * @param {MouseEvent} e - MouseEvent - The event object that was triggered by the click.
     * @param {number} [index=null] - number = null
     * @returns false
     */
    public handleTabClick(e: MouseEvent, index: number = null): boolean {
        if (index === null || index === 0) {
            e.preventDefault();
            e.stopPropagation();
        } else {
            this.updateActiveTabIndex(index);
        }

        return false;
    }

    /**
     * It takes a string as an argument and sets the value of the active tab to that string
     * @param {string} text - The text to be updated in the textarea
     * @memberof DotContentEditorComponent
     */
    updateContentTypeText(text: string): void {
        const control = this.containerContents.controls[this.activeTabIndex - 1].value;
        this.containerContents.controls[this.activeTabIndex - 1].setValue({
            ...control,
            code: text
        });
    }

    /**
     * It removes the form control at the index of the form array, and then closes the modal
     * @param {number} [index=null] - number = null
     * @param close - This is the function that closes the modal.
     * @memberof DotContentEditorComponent
     */
    handleClose(index: number = null, close: () => void): void {
        this.containerContents.removeAt(index - 1);
        close();
    }

    /**
     * It opens a dialog with a form to add a variable to the container
     * @param {DotContainerContent} contentType - DotContainerContent - The content type object that contains
     * the variables.
     * @returns {void}
     * @param {number} index - The index of the tab that was clicked.
     * @memberof DotContentEditorComponent
     */
    handleAddVariable(contentType: DotContainerContent) {
        this.dialogService.open(DotAddVariableComponent, {
            header: this.dotMessageService.get('containers.properties.add.variable.title'),
            width: '50rem',
            data: {
                contentTypeVariable: contentType.variable,
                onSave: (variable) => {
                    const editor = this.monacoEditors[contentType.variable].getModel();
                    this.monacoEditors[contentType.variable]
                        .getModel()
                        .setValue(editor.getValue() + `${variable}`);
                }
            }
        });
    }

    /**
     * It pushes the monaco instance into the monacoEditor array.
     * @param monacoInstance - The monaco instance that is created by the component.
     * @memberof DotContentEditorComponent
     */
    monacoInit(monacoEditor) {
        this.monacoEditors[monacoEditor.name] = monacoEditor.editor;
    }

    private init(): void {
        this.menuItems = this.mapMenuItems(this.contentTypes);
    }

    /**
     * It updates the activeTabIndex property with the index of the tab that was clicked
     * @param {number} index - number - The index of the tab that was clicked.
     * @memberof DotContentEditorComponent
     */
    private updateActiveTabIndex(index: number): void {
        this.activeTabIndex = index;
    }

    private mapMenuItems(contentTypes: DotContainerContent[]): MenuItem[] {
        return contentTypes.map((contentType) => {
            return {
                label: contentType.name,
                command: () => {
                    if (!this.checkIfAlreadyExists(contentType)) {
                        this.containerContents.push(
                            new FormControl<DotContainerContent>(contentType)
                        );
                    }
                }
            };
        });
    }

    private checkIfAlreadyExists(contentType: DotContainerContent): boolean {
        return this.containerContents.controls.some(
            (control) => control.value.variable === contentType.variable
        );
    }
}
