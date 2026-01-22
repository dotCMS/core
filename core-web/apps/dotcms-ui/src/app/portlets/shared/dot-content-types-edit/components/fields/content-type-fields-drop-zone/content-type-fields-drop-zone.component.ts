import autoScroll from 'dom-autoscroller';
import { DragulaService } from 'ng2-dragula';
import { Subject } from 'rxjs';

import {
    Component,
    ElementRef,
    OnChanges,
    OnDestroy,
    OnInit,
    Renderer2,
    SimpleChanges,
    effect,
    inject,
    input,
    output,
    viewChild
} from '@angular/core';

import { takeUntil } from 'rxjs/operators';

import { DotEventsService, DotMessageService } from '@dotcms/data-access';
import {
    DotCMSClazzes,
    DotCMSContentType,
    DotCMSContentTypeField,
    DotCMSContentTypeLayoutColumn,
    DotCMSContentTypeLayoutRow,
    DotDialogActions
} from '@dotcms/dotcms-models';
import { DotLoadingIndicatorService, FieldUtil } from '@dotcms/utils';

import { ContentTypeFieldsPropertiesFormComponent } from '../content-type-fields-properties-form';
import { FieldType } from '../models';
import { DropFieldData, FieldDragDropService } from '../service';
import { FieldPropertyService } from '../service/field-properties.service';

/**
 * Display all the Field Types
 *
 * @export
 * @class ContentTypeFieldsDropZoneComponent
 */
@Component({
    selector: 'dot-content-type-fields-drop-zone',
    templateUrl: './content-type-fields-drop-zone.component.html',
    standalone: false
})
export class ContentTypeFieldsDropZoneComponent implements OnInit, OnChanges, OnDestroy {
    private dotMessageService = inject(DotMessageService);
    private fieldDragDropService = inject(FieldDragDropService);
    private fieldPropertyService = inject(FieldPropertyService);
    private dotEventsService = inject(DotEventsService);
    private dotLoadingIndicatorService = inject(DotLoadingIndicatorService);
    private dragulaService = inject(DragulaService);
    private elRef = inject(ElementRef);
    private rendered = inject(Renderer2);

    readonly OVERVIEW_TAB_INDEX = 0;
    readonly BLOCK_EDITOR_SETTINGS_TAB_INDEX = 1;

    displayDialog = false;
    currentField: DotCMSContentTypeField;
    currentFieldType: FieldType;
    dialogActions: DotDialogActions;
    defaultDialogActions: DotDialogActions;
    fieldRows: DotCMSContentTypeLayoutRow[];
    hideButtons = false;
    activeTab = 0;

    readonly $propertiesForm =
        viewChild.required<ContentTypeFieldsPropertiesFormComponent>('fieldPropertiesForm');

    readonly $layout = input<DotCMSContentTypeLayoutRow[]>(undefined, { alias: 'layout' });
    readonly $contentType = input<DotCMSContentType>(undefined, { alias: 'contentType' });

    readonly saveFields = output<DotCMSContentTypeLayoutRow[]>();
    readonly editField = output<DotCMSContentTypeField>();
    readonly removeFields = output<DotCMSContentTypeField[]>();

    private destroy$: Subject<boolean> = new Subject<boolean>();

    private _loading: boolean;

    get loading(): boolean {
        return this._loading;
    }

    readonly $loading = input<boolean>(false, { alias: 'loading' });

    constructor() {
        effect(() => {
            const loading = this.$loading();
            if (loading) {
                this.dotLoadingIndicatorService.show();
            } else {
                this.dotLoadingIndicatorService.hide();
            }
        });
    }

    get isFieldWithSettings() {
        return [
            'com.dotcms.contenttype.model.field.ImmutableStoryBlockField',
            'com.dotcms.contenttype.model.field.ImmutableBinaryField'
        ].includes(this.currentFieldType?.clazz);
    }

    private static findColumnBreakIndex(fields: DotCMSContentTypeField[]): number {
        return fields.findIndex((item: DotCMSContentTypeField) => {
            return item.clazz === DotCMSClazzes.COLUMN_BREAK;
        });
    }

    private static splitColumnsInRows(
        fieldRows: DotCMSContentTypeLayoutRow[]
    ): DotCMSContentTypeLayoutRow[] {
        return fieldRows.map((row: DotCMSContentTypeLayoutRow) => {
            return {
                ...row,
                columns: row.columns.reduce(
                    (
                        acc: DotCMSContentTypeLayoutColumn[],
                        current: DotCMSContentTypeLayoutColumn
                    ) => {
                        const indexOfBreak =
                            ContentTypeFieldsDropZoneComponent.findColumnBreakIndex(current.fields);
                        if (indexOfBreak > -1) {
                            const firstColFields = current.fields.slice(0, indexOfBreak);
                            const secondColFields = current.fields.slice(indexOfBreak + 1);

                            const firstCol = {
                                ...current,
                                fields: firstColFields
                            };

                            const secondCol = FieldUtil.createFieldColumn(secondColFields);

                            return [...acc, firstCol, secondCol];
                        } else {
                            return acc.concat(current);
                        }
                    },
                    []
                )
            };
        });
    }

    ngOnInit(): void {
        this.defaultDialogActions = {
            accept: {
                action: () => {
                    this.$propertiesForm().saveFieldProperties();
                },
                label: this.dotMessageService.get('contenttypes.dropzone.action.save'),
                disabled: true
            },
            cancel: {
                label: this.dotMessageService.get('contenttypes.dropzone.action.cancel'),
                action: () => {
                    this.removeFieldsWithoutId();
                    this.displayDialog = false;
                }
            }
        };

        this.dialogActions = this.defaultDialogActions;

        this.fieldDragDropService.fieldDropFromSource$
            .pipe(takeUntil(this.destroy$))
            .subscribe((data: DropFieldData) => {
                if (FieldUtil.isColumnBreak(data.item.clazz)) {
                    setTimeout(() => {
                        this.emitSaveFields(
                            ContentTypeFieldsDropZoneComponent.splitColumnsInRows(this.fieldRows)
                        );
                    }, 0);
                } else {
                    this.setDroppedField(data.item);
                    this.toggleDialog();
                }
            });

        this.fieldDragDropService.fieldDropFromTarget$
            .pipe(takeUntil(this.destroy$))
            .subscribe(() => {
                setTimeout(() => {
                    this.emitSaveFields(this.fieldRows);
                }, 0);
            });

        this.fieldDragDropService.fieldRowDropFromTarget$
            .pipe(takeUntil(this.destroy$))
            .subscribe((fieldRows: DotCMSContentTypeLayoutRow[]) => {
                this.fieldRows = fieldRows;
                this.emitSaveFields(fieldRows);
            });

        this.dotEventsService
            .listen('add-row')
            .pipe(takeUntil(this.destroy$))
            .subscribe(() => {
                document.querySelector('dot-add-rows').scrollIntoView({
                    behavior: 'smooth'
                });
            });

        this.dotEventsService
            .listen('add-tab-divider')
            .pipe(takeUntil(this.destroy$))
            .subscribe(() => {
                const fieldTab: DotCMSContentTypeLayoutRow = FieldUtil.createFieldTabDivider();
                this.fieldRows.push(fieldTab);
                this.setDroppedField(fieldTab.divider);
                this.toggleDialog();
            });

        this.setUpDragulaScroll();
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.$layout && changes.$layout.currentValue) {
            this.fieldRows = structuredClone(changes.$layout.currentValue);
        }
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    /**
     * Adds row to the layout of content type
     * @param number columns new row's number of columns
     * @memberof ContentTypeFieldsDropZoneComponent
     */
    addRow(columns: number): void {
        const newRow = FieldUtil.createFieldRow(columns);
        this.fieldRows.push(newRow);
    }

    /**
     * Convert WYSIWYG field to Block Field
     *
     * @memberof ContentTypeFieldsDropZoneComponent
     */
    convertWysiwygToBlock() {
        this.editField.emit({
            ...this.currentField,
            clazz: 'com.dotcms.contenttype.model.field.ImmutableStoryBlockField',
            fieldType: 'Story-Block'
        });
        this.toggleDialog();
    }

    /**
     * Emit the saveField event
     * @param DotContentTypeField fieldToSave
     * @memberof ContentTypeFieldsDropZoneComponent
     */
    saveFieldsHandler(fieldToSave: DotCMSContentTypeField): void {
        if (!this.currentField) {
            const tabDividerFields = FieldUtil.getTabDividerFields(this.fieldRows);
            this.currentField = tabDividerFields.find(
                (field: DotCMSContentTypeField) => fieldToSave.id === field.id
            );
        }

        Object.assign(this.currentField, fieldToSave);

        if (fieldToSave.id) {
            this.editField.emit(this.currentField);
        } else {
            this.emitSaveFields(this.fieldRows);
        }

        this.toggleDialog();
    }

    /**
     * Get the field to be edited
     * @param DotContentTypeField fieldToEdit
     * @memberof ContentTypeFieldsDropZoneComponent
     */
    editFieldHandler(fieldToEdit: DotCMSContentTypeField): void {
        if (!this.fieldDragDropService.isDraggedEventStarted()) {
            const fields = FieldUtil.getFieldsWithoutLayout(this.fieldRows);
            this.currentField = fields.find(
                (field: DotCMSContentTypeField) => fieldToEdit.id === field.id
            );
            this.currentFieldType = this.fieldPropertyService.getFieldType(this.currentField.clazz);
            this.toggleDialog();
        }
    }

    /**
     * Removes the last dropped field added without ID
     * @memberof ContentTypeFieldsDropZoneComponent
     */
    removeFieldsWithoutId(): void {
        const fieldRows: DotCMSContentTypeLayoutRow[] = this.fieldRows;

        // TODO needs an improvement for performance reasons
        fieldRows.forEach((row, rowIndex) => {
            if (row.columns) {
                row.columns.forEach((col, colIndex) => {
                    col.fields.forEach((field, fieldIndex) => {
                        if (!field.id) {
                            row.columns[colIndex].fields.splice(fieldIndex, 1);
                        }
                    });
                });
            } else if (!row.divider.name) {
                this.fieldRows.splice(rowIndex, 1);
            }
        });
        this.hideButtons = false;
        this.activeTab = 0;
        this.displayDialog = false;
        this.currentField = null;
        this.setDialogOkButtonState(false);
    }

    /**
     * Trigger the removeFields event with fieldToDelete
     * @param {DotCMSContentTypeField} fieldToDelete
     * @memberof ContentTypeFieldsDropZoneComponent
     */
    removeField(fieldToDelete: DotCMSContentTypeField): void {
        this.removeFields.emit([fieldToDelete]);
    }

    /**
     * Trigger the removeFields event with all the fields in fieldRow
     * @param {DotCMSContentTypeLayoutRow} fieldRow
     * @memberof ContentTypeFieldsDropZoneComponent
     */
    removeFieldRow(fieldRow: DotCMSContentTypeLayoutRow, index: number): void {
        this.fieldRows.splice(index, 1);
        const fieldsToDelete: DotCMSContentTypeField[] = [];

        if (!FieldUtil.isNewField(fieldRow.divider)) {
            fieldsToDelete.push(fieldRow.divider);
            fieldRow.columns.forEach((fieldColumn: DotCMSContentTypeLayoutColumn) => {
                fieldsToDelete.push(fieldColumn.columnDivider);
                fieldColumn.fields.forEach((field) => fieldsToDelete.push(field));
            });
            this.removeFields.emit(fieldsToDelete);
        }
    }

    /**
     * Trigger the removeFields event with the tab to be removed
     * @param {DotCMSContentTypeLayoutRow} fieldTab
     * @memberof ContentTypeFieldsDropZoneComponent
     */
    removeTab(fieldTab: DotCMSContentTypeLayoutRow, index: number): void {
        this.fieldRows.splice(index, 1);
        this.removeFields.emit([fieldTab.divider]);
    }

    /**
     * Cancel the last drag and drop operation
     *
     * @memberof ContentTypeFieldsDropZoneComponent
     */
    cancelLastDragAndDrop(): void {
        this.fieldRows = structuredClone(this.$layout());
    }

    /**
     * Set the state for the ok action for the dialog
     *
     * @param {boolean} $event
     * @memberof ContentTypeFieldsDropZoneComponent
     */
    setDialogOkButtonState(formChanged: boolean): void {
        this.dialogActions = {
            ...this.dialogActions,
            accept: {
                ...this.dialogActions.accept,
                disabled: !formChanged
            }
        };
    }

    /**
     * Hide or show the 'save' and 'hide' buttons according to the field tab selected
     *
     * @param index
     */
    handleTabChange(index: number): void {
        if (index === this.OVERVIEW_TAB_INDEX) {
            this.dialogActions = this.defaultDialogActions;
        }

        this.hideButtons =
            index !== this.OVERVIEW_TAB_INDEX &&
            !(index === this.BLOCK_EDITOR_SETTINGS_TAB_INDEX && this.isFieldWithSettings);
    }

    /**
     * Scroll into convert to block section
     *
     * @memberof ContentTypeFieldsDropZoneComponent
     */
    scrollTo() {
        const el = this.rendered.selectRootElement('dot-convert-wysiwyg-to-block', true);

        el.scrollIntoView({
            behavior: 'smooth',
            block: 'start',
            inline: 'nearest'
        });
    }

    /**
     * Change dialogActions
     *
     * @param {DotDialogActions} controls
     * @memberof ContentTypeFieldsDropZoneComponent
     */
    changesDialogActions(controls: DotDialogActions) {
        this.dialogActions = controls;
    }

    handleDialogVisibleChange(isVisible: boolean): void {
        if (!isVisible) {
            this.removeFieldsWithoutId();
        }
    }

    protected toggleDialog(): void {
        this.dialogActions = this.defaultDialogActions;
        this.activeTab = this.OVERVIEW_TAB_INDEX;
        this.displayDialog = !this.displayDialog;
    }

    private setDroppedField(droppedField: DotCMSContentTypeField): void {
        this.currentField = droppedField;
        this.currentFieldType = this.fieldPropertyService.getFieldType(this.currentField.clazz);
    }

    private emitSaveFields(layout: DotCMSContentTypeLayoutRow[]): void {
        this.saveFields.emit(layout);
    }

    private setUpDragulaScroll(): void {
        const drake = this.dragulaService.find('fields-bag')?.drake;
        autoScroll([this.elRef.nativeElement.parentElement], {
            margin: 100,
            maxSpeed: 60,
            scrollWhenOutside: true,
            autoScroll() {
                return this.down && drake.dragging;
            }
        });
    }
}
