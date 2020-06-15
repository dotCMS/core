import {
    Component,
    SimpleChanges,
    Input,
    Output,
    EventEmitter,
    OnInit,
    OnChanges,
    OnDestroy,
    ViewChild
} from '@angular/core';
import { FieldDragDropService, DropFieldData } from '../service';
import { FieldType } from '../models';
import {
    DotCMSContentTypeField,
    DotCMSContentTypeLayoutRow,
    DotCMSContentTypeLayoutColumn
} from 'dotcms-models';
import { ContentTypeFieldsPropertiesFormComponent } from '../content-type-fields-properties-form';
import { DotMessageService } from '@services/dot-messages-service';
import { FieldUtil } from '../util/field-util';
import { FieldPropertyService } from '../service/field-properties.service';
import { DotDialogActions } from '@components/dot-dialog/dot-dialog.component';
import { DotEventsService } from '@services/dot-events/dot-events.service';
import { takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';
import { DotLoadingIndicatorService } from '@components/_common/iframe/dot-loading-indicator/dot-loading-indicator.service';
import * as _ from 'lodash';

/**
 * Display all the Field Types
 *
 * @export
 * @class ContentTypeFieldsDropZoneComponent
 */
@Component({
    selector: 'dot-content-type-fields-drop-zone',
    styleUrls: ['./content-type-fields-drop-zone.component.scss'],
    templateUrl: './content-type-fields-drop-zone.component.html'
})
export class ContentTypeFieldsDropZoneComponent implements OnInit, OnChanges, OnDestroy {
    readonly OVERVIEW_TAB_INDEX = 0;

    dialogActiveTab: number;
    displayDialog = false;
    currentField: DotCMSContentTypeField;
    currentFieldType: FieldType;
    dialogActions: DotDialogActions;
    fieldRows: DotCMSContentTypeLayoutRow[];

    @ViewChild('fieldPropertiesForm')
    propertiesForm: ContentTypeFieldsPropertiesFormComponent;

    @Input()
    layout: DotCMSContentTypeLayoutRow[];

    @Output()
    saveFields = new EventEmitter<DotCMSContentTypeLayoutRow[]>();

    @Output()
    editField = new EventEmitter<DotCMSContentTypeField>();

    @Output()
    removeFields = new EventEmitter<DotCMSContentTypeField[]>();

    hideButtons = false;
    private _loading: boolean;
    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        private dotMessageService: DotMessageService,
        private fieldDragDropService: FieldDragDropService,
        private fieldPropertyService: FieldPropertyService,
        private dotEventsService: DotEventsService,
        private dotLoadingIndicatorService: DotLoadingIndicatorService
    ) {}

    private static findColumnBreakIndex(fields: DotCMSContentTypeField[]): number {
        return fields.findIndex((item: DotCMSContentTypeField) => {
            return FieldUtil.isColumnBreak(item.clazz);
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
                        const indexOfBreak = ContentTypeFieldsDropZoneComponent.findColumnBreakIndex(
                            current.fields
                        );
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
        this.dialogActions = {
            accept: {
                action: () => {
                    this.propertiesForm.saveFieldProperties();
                },
                label: this.dotMessageService.get('contenttypes.dropzone.action.save'),
                disabled: true
            },
            cancel: {
                label: this.dotMessageService.get('contenttypes.dropzone.action.cancel'),
                action: () => {}
            }
        };

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
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.layout && changes.layout.currentValue) {
            this.fieldRows = _.cloneDeep(changes.layout.currentValue);
        }
    }

    @Input()
    set loading(loading: boolean) {
        this._loading = loading;

        if (loading) {
            this.dotLoadingIndicatorService.show();
        } else {
            this.dotLoadingIndicatorService.hide();
        }
    }

    get loading(): boolean {
        return this._loading;
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

        if (!!fieldToSave.id) {
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
        const fieldRows: any = this.fieldRows;

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
        this.displayDialog = false;
        this.currentField = null;
        this.dialogActiveTab = null;
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
        this.fieldRows = _.cloneDeep(this.layout);
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
        this.hideButtons = index !== this.OVERVIEW_TAB_INDEX;
    }

    private setDroppedField(droppedField: DotCMSContentTypeField): void {
        this.currentField = droppedField;
        this.currentFieldType = this.fieldPropertyService.getFieldType(this.currentField.clazz);
    }

    private toggleDialog(): void {
        this.displayDialog = !this.displayDialog;
        this.dialogActiveTab = 0;
    }

    private emitSaveFields(layout: DotCMSContentTypeLayoutRow[]): void {
        this.saveFields.emit(layout);
    }
}
