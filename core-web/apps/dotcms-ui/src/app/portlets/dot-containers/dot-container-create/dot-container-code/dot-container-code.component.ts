import { MonacoStandaloneCodeEditor } from '@materia-ui/ngx-monaco-editor';

import { trigger, transition, style, animate } from '@angular/animations';
import { Component, Input, OnChanges, OnInit, SimpleChanges, inject } from '@angular/core';
import { FormArray, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { MenuItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DialogService, DynamicDialogModule } from 'primeng/dynamicdialog';
import { MenuModule } from 'primeng/menu';
import { SkeletonModule } from 'primeng/skeleton';
import { TabsModule } from 'primeng/tabs';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSContentType } from '@dotcms/dotcms-models';
import { DotFieldRequiredDirective, DotMessagePipe } from '@dotcms/ui';

import { DotAddVariableComponent } from './dot-add-variable/dot-add-variable.component';

import { DotTextareaContentComponent } from '../../../../view/components/_common/dot-textarea-content/dot-textarea-content.component';

interface DotContainerContent extends DotCMSContentType {
    code: string;
    structureId: string;
    containerId?: string;
    containerInode?: string;
    contentTypeVar?: string;
}

@Component({
    animations: [
        trigger('contentCodeAnimation', [
            transition(':enter', [style({ opacity: 0 }), animate(500, style({ opacity: 1 }))])
        ])
    ],
    selector: 'dot-container-code',
    templateUrl: './dot-container-code.component.html',
    imports: [
        ReactiveFormsModule,
        TabsModule,
        MenuModule,
        DotTextareaContentComponent,
        DotMessagePipe,
        ButtonModule,
        DynamicDialogModule,

        SkeletonModule,
        DotFieldRequiredDirective
    ],
    providers: [DialogService]
})
export class DotContentEditorComponent implements OnInit, OnChanges {
    private dialogService = inject(DialogService);
    private dotMessageService = inject(DotMessageService);

    @Input() fg: FormGroup;
    @Input() contentTypes: DotCMSContentType[];

    menuItems: MenuItem[];
    activeTabIndex = 0;
    monacoEditors: Record<string, MonacoStandaloneCodeEditor> = {};
    contentTypeNamesById = {};

    ngOnInit() {
        if (this.contentTypes && this.contentTypes.length > 0) {
            this.contentTypes.forEach(({ id, name }: DotCMSContentType) => {
                this.contentTypeNamesById[id] = name;
            });
        }

        this.init();
        this.updateActiveTabIndex(this.getcontainerStructures.length);
    }

    ngOnChanges(changes: SimpleChanges) {
        if (changes.contentTypes?.currentValue?.length > 0) {
            changes.contentTypes.currentValue.forEach(({ id, name }: DotCMSContentType) => {
                this.contentTypeNamesById[id] = name;
            });

            this.init();
            this.updateActiveTabIndex(this.getcontainerStructures.length);

            Object.keys(this.monacoEditors).forEach((editorId) => {
                this.monacoEditors[editorId].updateOptions({ readOnly: false });
            });
        }
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
     * Handles tab change from p-tabs valueChange event
     * @param {number} value - The new tab index value
     * @memberof DotContentEditorComponent
     */
    public handleTabChange(value: number): void {
        if (value !== 0) {
            this.updateActiveTabIndex(value);
            this.focusCurrentEditor(value);
        }
    }

    /**
     * If the index is null or 0, prevent the default action and stop propagation. Otherwise, update the active tab index
     * and push the container content
     * @param event - The click event (MouseEvent)
     * @param {number} [index=null] - number = null
     * @returns false
     */
    public handleTabClick(event: MouseEvent, index: number = null): boolean {
        if (index === 0) {
            event.preventDefault();
            event.stopPropagation();
        } else if (index !== null) {
            this.updateActiveTabIndex(index);
            this.focusCurrentEditor(index);
        }

        return false;
    }

    /**
     * It removes the form control at the index of the form array
     * @param {number} [index=null] - number = null
     * @memberof DotContentEditorComponent
     */
    removeItem(index: number = null): void {
        if (this.contentTypes.length > 0) {
            this.getcontainerStructures.removeAt(index - 1);
            const currentTabIndex = this.findCurrentTabIndex(index);
            this.updateActiveTabIndex(currentTabIndex);
            this.focusCurrentEditor(currentTabIndex);
        }
    }

    /**
     * Focus current editor
     * @param {number} tabIdx
     * @memberof DotContentEditorComponent
     */
    focusCurrentEditor(tabIdx: number) {
        if (tabIdx > 0) {
            const contentTypeId =
                this.getcontainerStructures.controls[tabIdx - 1].get('structureId').value;
            // Tab Panel does not trigger any event after completely rendered.
            // Tab Panel and Monaco-Editor take sometime to render it completely.
            requestAnimationFrame(() => {
                this.monacoEditors[contentTypeId].focus();
            });
        }
    }

    /**
     * Find current tab after deleting content type
     * @param {*} index
     * @return {*}  {number}
     * @memberof DotContentEditorComponent
     */
    findCurrentTabIndex(index): number {
        // -1 in condition because if it is first tab then no need to minus
        return index - 1 > 0 ? index - 1 : this.getcontainerStructures.length > 0 ? index : 0;
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
            width: '25rem',
            contentStyle: { padding: '0' },
            closable: true,
            header: this.dotMessageService.get('Add-Variables'),
            data: {
                contentTypeVariable: contentType.structureId,
                onSave: (codeTemplate: string) => {
                    const editor = this.monacoEditors[contentType.structureId];

                    const selections = editor.getSelections();

                    const editOperation = selections.map((selection) => {
                        return {
                            range: {
                                startLineNumber: selection.startLineNumber,
                                startColumn: selection.startColumn,
                                endLineNumber: selection.endLineNumber,
                                endColumn: selection.endColumn
                            },
                            text: codeTemplate
                        };
                    });

                    editor.getModel().pushEditOperations(selections, editOperation, () => {
                        return null;
                    });
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
        if (this.contentTypes.length === 0) {
            this.monacoEditors[monacoEditor.name].updateOptions({ readOnly: true });
        }

        requestAnimationFrame(() => this.monacoEditors[monacoEditor.name].focus());
    }

    private init(): void {
        this.menuItems = this.getMenuItems(this.contentTypes || []);

        // default content type if content type does not exist
        if (this.getcontainerStructures.length === 0 && this.contentTypes?.length > 0) {
            this.getcontainerStructures.push(
                new FormGroup({
                    code: new FormControl(''),
                    structureId: new FormControl(this.contentTypes[0].id, [Validators.required])
                })
            );
        }
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
                                code: new FormControl(''),
                                structureId: new FormControl(contentType.id, [Validators.required])
                            })
                        );

                        // Waiting for primeng to add the tabPanel
                        requestAnimationFrame(() => {
                            this.updateActiveTabIndex(this.getcontainerStructures.length);
                        });
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
