import autoScroll from 'dom-autoscroller';
import { DragulaService } from 'ng2-dragula';
import { Subject } from 'rxjs';

import { DialogService } from 'primeng/dynamicdialog';

import {
    Component,
    ElementRef,
    OnChanges,
    OnDestroy,
    OnInit,
    SimpleChanges,
    inject,
    input,
    output
} from '@angular/core';

import { take, takeUntil } from 'rxjs/operators';

import { DotEventsService } from '@dotcms/data-access';
import {
    DotCMSClazzes,
    DotCMSContentType,
    DotCMSContentTypeField,
    DotCMSContentTypeLayoutColumn,
    DotCMSContentTypeLayoutRow
} from '@dotcms/dotcms-models';
import { DotLoadingIndicatorService, FieldUtil } from '@dotcms/utils';

import { DotEditFieldDialogComponent, DotEditFieldDialogResult } from '../dot-edit-field-dialog';
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
    standalone: false,
    providers: [DialogService]
})
export class ContentTypeFieldsDropZoneComponent implements OnInit, OnChanges, OnDestroy {
    private fieldDragDropService = inject(FieldDragDropService);
    private fieldPropertyService = inject(FieldPropertyService);
    private dotEventsService = inject(DotEventsService);
    private dotLoadingIndicatorService = inject(DotLoadingIndicatorService);
    private dragulaService = inject(DragulaService);
    private dialogService = inject(DialogService);
    private elRef = inject(ElementRef);

    currentField: DotCMSContentTypeField;
    currentFieldType: FieldType;
    fieldRows: DotCMSContentTypeLayoutRow[];

    /** Layout rows used to render the drop-zone. Changes trigger a structural clone. */
    readonly $layout = input<DotCMSContentTypeLayoutRow[]>(undefined, { alias: 'layout' });

    /** Content type that owns the fields being edited. */
    readonly $contentType = input<DotCMSContentType>(undefined, { alias: 'contentType' });

    /** Emits the updated layout after a field is saved or a drag-drop reorder occurs. */
    readonly saveFields = output<DotCMSContentTypeLayoutRow[]>();

    /** Emits the field after it has been updated (edit with existing id). */
    readonly editField = output<DotCMSContentTypeField>();

    /** Emits the list of fields to be deleted. */
    readonly removeFields = output<DotCMSContentTypeField[]>();

    private destroy$: Subject<boolean> = new Subject<boolean>();

    private _loading = false;

    get loading(): boolean {
        return this._loading;
    }

    /** When `true`, shows the global loading indicator over the drop-zone. */
    readonly $loading = input<boolean>(false, { alias: 'loading' });

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
                    this.openFieldDialog();
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
                this.openFieldDialog();
            });

        this.setUpDragulaScroll();
    }

    /**
     * Open the field-edit dialog and react to its result.
     * On save the field is committed, on convert-to-block the field is emitted,
     * and on cancel/dismiss any field added without an id is removed.
     */
    private openFieldDialog(): void {
        const ref = this.dialogService.open(DotEditFieldDialogComponent, {
            header: this.currentFieldType?.label,
            modal: true,
            width: '45rem',
            closable: true,
            closeOnEscape: true,
            data: {
                currentField: this.currentField,
                currentFieldType: this.currentFieldType,
                contentType: this.$contentType()
            }
        });

        ref.onClose.subscribe((result?: DotEditFieldDialogResult) => {
            switch (result?.kind) {
                case 'saved':
                    this.saveFieldsHandler(result.field);
                    break;
                case 'convert-to-block':
                    this.editField.emit(result.field);
                    break;
                case 'settings-saved':
                    break;
                default:
                    this.removeFieldsWithoutId();
            }
        });
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.$layout && changes.$layout.currentValue) {
            this.fieldRows = structuredClone(changes.$layout.currentValue);
        }

        if (changes.$loading) {
            const loading = changes.$loading.currentValue;
            this._loading = loading;

            // Use setTimeout to defer loading indicator changes until after current change detection cycle
            setTimeout(() => {
                if (loading) {
                    this.dotLoadingIndicatorService.show();
                } else {
                    this.dotLoadingIndicatorService.hide();
                }
            }, 0);
        }
    }

    ngOnDestroy(): void {
        this.dotLoadingIndicatorService.hide();
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

        if (fieldToSave.id) {
            this.editField.emit(this.currentField);
        } else {
            this.emitSaveFields(this.fieldRows);
        }
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
            this.openFieldDialog();
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
        this.currentField = null;
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
