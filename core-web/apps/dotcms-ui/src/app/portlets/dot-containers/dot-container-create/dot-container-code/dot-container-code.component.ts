import { Component, Input, OnInit } from '@angular/core';
import { DialogService } from 'primeng/dynamicdialog';
import { DotAddVariableComponent } from './dot-add-variable/dot-add-variable.component';
import { DotMessageService } from '@dotcms/app/api/services/dot-message/dot-messages.service';
import { DotCMSContentType } from '@dotcms/dotcms-models';
import { FormArray, FormControl, FormGroup, Validators } from '@angular/forms';
import { MenuItem } from 'primeng/api';

interface DotContainerContent extends DotCMSContentType {
    code: string;
    structureId: string;
    containerId?: string;
    containerInode?: string;
    contentTypeVar?: string;
}

@Component({
    selector: 'dot-container-code',
    templateUrl: './dot-container-code.component.html',
    styleUrls: ['./dot-container-code.component.scss']
})
export class DotContentEditorComponent implements OnInit {
    @Input() fg: FormGroup;
    @Input() contentTypes: DotCMSContentType[];

    menuItems: MenuItem[];
    activeTabIndex = 0;
    monacoEditors = {};
    contentTypeNamesById = {};

    constructor(
        private dialogService: DialogService,
        private dotMessageService: DotMessageService
    ) {}

    ngOnInit() {
        this.contentTypes.forEach(({ id, name }: DotCMSContentType) => {
            this.contentTypeNamesById[id] = name;
        });

        this.updateActiveTabIndex(this.getcontainerStructures.length);
        this.init();
    }

    /**
     * Get ContainerStructure as FormArray
     * @readonly
     * @type {FormArray}
     * @memberof DotContentEditorComponent
     */
    get getcontainerStructures(): FormArray {
        return this.fg.get('containerStructures') as FormArray;
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
            this.foucsCurrentEditor(index);
        }

        return false;
    }

    /**
     * It removes the form control at the index of the form array, and then closes the modal
     * @param {number} [index=null] - number = null
     * @param close - This is the function that closes the modal.
     * @memberof DotContentEditorComponent
     */
    removeItem(index: number = null): void {
        this.getcontainerStructures.removeAt(index - 1);
        const currentTabIndex = this.findCurrentTabIndex(index);
        this.updateActiveTabIndex(currentTabIndex);
        this.foucsCurrentEditor(currentTabIndex);
    }

    /**
     * Focus current editor
     * @param {number} tabIdx
     * @memberof DotContentEditorComponent
     */
    foucsCurrentEditor(tabIdx: number) {
        if (tabIdx > 0) {
            const contentTypeId =
                this.getcontainerStructures.controls[tabIdx - 1].get('structureId').value;
            // Tab Panel does not trigger any event after completely rendered.
            // Tab Panel and Monaco-Editor take sometime to render it completely.
            setTimeout(() => {
                this.monacoEditors[contentTypeId].focus();
            }, 0);
        }
    }

    /**
     * Find current tab after deleting content type
     * @param {*} index
     * @return {*}  {number}
     * @memberof DotContentEditorComponent
     */
    findCurrentTabIndex(index): number {
        let currentIndex = index - 1;

        if (currentIndex > 0) {
            // do nothing
        } else if (this.getcontainerStructures.length > 0) {
            currentIndex = currentIndex + 1;
        } else {
            // empty tab index
            currentIndex = 0;
        }

        return currentIndex;
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
                contentTypeVariable: contentType.structureId,
                onSave: (variable) => {
                    const editor = this.monacoEditors[contentType.structureId].getModel();
                    this.monacoEditors[contentType.structureId]
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
        this.monacoEditors[monacoEditor.name].focus();
    }

    private init(): void {
        this.menuItems = this.getMenuItems(this.contentTypes);
    }

    /**
     * It updates the activeTabIndex property with the index of the tab that was clicked
     * @param {number} index - number - The index of the tab that was clicked.
     * @memberof DotContentEditorComponent
     */
    private updateActiveTabIndex(index: number): void {
        this.activeTabIndex = index;
    }

    private getMenuItems(contentTypes: DotCMSContentType[]): MenuItem[] {
        return contentTypes.map((contentType) => {
            return {
                label: contentType.name,
                command: () => {
                    if (!this.checkIfAlreadyExists(contentType)) {
                        this.getcontainerStructures.push(
                            new FormGroup({
                                code: new FormControl('', [Validators.required]),
                                structureId: new FormControl(contentType.id, [Validators.required])
                            })
                        );

                        // Waiting for primeng to add the tabPanel
                        setTimeout(() => {
                            this.updateActiveTabIndex(this.getcontainerStructures.length);
                        }, 0);
                    }
                }
            };
        });
    }

    private checkIfAlreadyExists(contentType: DotCMSContentType): boolean {
        return this.getcontainerStructures.controls.some(
            (control) => control.value.structureId === contentType.id
        );
    }
}
