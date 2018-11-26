import {
    Component,
    SimpleChanges,
    Input,
    Output,
    EventEmitter,
    OnInit,
    OnChanges,
    ViewChild,
    OnDestroy
} from '@angular/core';
import { FieldDragDropService, DropFieldData } from '../service';
import { FieldRow, FieldTab, ContentTypeField, FieldType, FieldColumn } from '../shared';
import { ContentTypeFieldsPropertiesFormComponent } from '../content-type-fields-properties-form';
import { DotMessageService } from '@services/dot-messages-service';
import { FieldUtil } from '../util/field-util';
import { FieldPropertyService } from '../service/field-properties.service';
import { DotDialogActions } from '@components/dot-dialog/dot-dialog.component';
import { DotEventsService } from '@services/dot-events/dot-events.service';
import { FieldDivider } from '@portlets/content-types/fields/shared/field-divider.interface';
import { takeUntil, take } from 'rxjs/operators';
import { Subject } from 'rxjs';
import { DotFieldVariableParams } from '../service/dot-field-variables.service';

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
    dialogActiveTab: number;
    displayDialog = false;
    fieldRows: FieldDivider[] = [];
    formData: ContentTypeField;
    currentFieldType: FieldType;
    currentField: DotFieldVariableParams;
    dialogActions: DotDialogActions;


    @ViewChild('fieldPropertiesForm')
    propertiesForm: ContentTypeFieldsPropertiesFormComponent;

    @ViewChild('fieldPropertiesForm')
    @Input()
    fields: ContentTypeField[];
    @Output()
    saveFields = new EventEmitter<ContentTypeField[]>();
    @Output()
    removeFields = new EventEmitter<ContentTypeField[]>();

    i18nMessages: {
        [key: string]: string;
    } = {};

    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        public dotMessageService: DotMessageService,
        private fieldDragDropService: FieldDragDropService,
        private fieldPropertyService: FieldPropertyService,
        private dotEventsService: DotEventsService
    ) {}

    ngOnInit(): void {
        this.dotMessageService
            .getMessages([
                'contenttypes.dropzone.action.save',
                'contenttypes.dropzone.action.cancel',
                'contenttypes.dropzone.action.edit',
                'contenttypes.dropzone.action.create.field',
                'contenttypes.dropzone.empty.message',
                'contenttypes.dropzone.tab.overview',
                'contenttypes.dropzone.tab.variables',
                'contenttypes.dropzone.empty.message'
            ])
            .pipe(take(1))
            .subscribe((messages: { [key: string]: string }) => {
                this.i18nMessages = messages;

                this.dialogActions = {
                    accept: {
                        action: () => {
                            this.propertiesForm.saveFieldProperties();
                        },
                        label: this.i18nMessages['contenttypes.dropzone.action.save']
                    },
                    cancel: {
                        label: this.i18nMessages['contenttypes.dropzone.action.cancel'],
                        action: () => {}
                    }
                };
            });

        this.fieldDragDropService.fieldDropFromSource$
            .pipe(takeUntil(this.destroy$))
            .subscribe((data: DropFieldData) => {
                this.setDroppedField(data.item);
                this.setModel(data.target);
                this.toggleDialog();
            });

        this.fieldDragDropService.fieldDropFromTarget$
            .pipe(takeUntil(this.destroy$))
            .subscribe((data: DropFieldData) => {
                this.setModel(data.target);

                if (data.source !== data.target) {
                    this.setModel(data.source);
                }

                this.moveFields();
            });

        this.fieldDragDropService.fieldRowDropFromTarget$
            .pipe(takeUntil(this.destroy$))
            .subscribe((fieldRows: FieldDivider[]) => {
                this.fieldRows = fieldRows;
                this.moveFields();
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
                const fieldTab: FieldTab = new FieldTab(FieldUtil.createFieldTabDivider());
                this.fieldRows.push(fieldTab);
                this.setDroppedField(fieldTab.getFieldDivider());
                this.toggleDialog();
            });
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.fields.currentValue) {
            const fields = changes.fields.currentValue;

            if (Array.isArray(fields)) {
                this.fieldRows = this.getRowFields(fields);
            } else {
                throw new Error('Fields attribute must be a Array');
            }
        }
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    /**
     * Adds columns to the layout of content type
     * @param number columns
     * @memberof ContentTypeFieldsDropZoneComponent
     */
    addRow(columns: number): void {
        this.fieldRows.push(new FieldRow(columns));
    }

    /**
     * Emit the saveField event
     * @param ContentTypeField fieldToSave
     * @memberof ContentTypeFieldsDropZoneComponent
     */
    saveFieldsHandler(fieldToSave: ContentTypeField): void {
        let fields: ContentTypeField[];

        if (fieldToSave.id) {
            fields = [fieldToSave];
        } else {
            fields = this.getFieldsToSave(fieldToSave);
            this.toggleDialog();
        }

        this.saveFields.emit(fields);
    }

    /**
     * Get the field to be edited
     * @param ContentTypeField fieldToEdit
     * @memberof ContentTypeFieldsDropZoneComponent
     */
    editField(fieldToEdit: ContentTypeField): void {
        const fields = this.getFields();
        this.formData = fields.filter((field) => fieldToEdit.id === field.id)[0];
        this.currentFieldType = this.fieldPropertyService.getFieldType(this.formData.clazz);
        this.currentField = {
            fieldId: this.formData.id,
            contentTypeId: this.formData.contentTypeId
        };
        this.toggleDialog();
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
            } else if (!row.fieldDivider.name) {
                this.fieldRows.splice(rowIndex, 1);
            }
        });
        this.displayDialog = false;
        this.formData = null;
        this.dialogActiveTab = null;
        this.propertiesForm.destroy();
    }

    /**
     * Trigger the removeFields event with fieldToDelete
     * @param {ContentTypeField} fieldToDelete
     * @memberof ContentTypeFieldsDropZoneComponent
     */
    removeField(fieldToDelete: ContentTypeField): void {
        this.removeFields.emit([fieldToDelete]);
    }

    /**
     * Trigger the removeFields event with all the fields in fieldRow
     * @param {FieldRow} fieldRow
     * @memberof ContentTypeFieldsDropZoneComponent
     */
    removeFieldRow(fieldRow: FieldRow): void {
        this.fieldRows.splice(this.fieldRows.indexOf(fieldRow), 1);
        const fieldsToDelete: ContentTypeField[] = [];
        const fieldDivider = fieldRow.getFieldDivider();

        if (!FieldUtil.isNewField(fieldDivider)) {
            fieldsToDelete.push(fieldDivider);
            fieldRow.columns.forEach((fieldColumn) => {
                fieldsToDelete.push(fieldColumn.columnDivider);
                fieldColumn.fields.forEach((field) => fieldsToDelete.push(field));
            });
            this.removeFields.emit(fieldsToDelete);
        }
    }

    /**
     * Trigger the removeFields event with the tab to be removed
     * @param {FieldTab} fieldTab
     * @memberof ContentTypeFieldsDropZoneComponent
     */
    removeTab(fieldTab: FieldTab): void {
        this.fieldRows.splice(this.fieldRows.indexOf(fieldTab), 1);
        this.removeFields.emit([fieldTab.getFieldDivider()]);
    }

    /**
     * Checks if field is Tab Divider
     * @param {FieldDivider} row
     * @returns {boolean}
     * @memberof ContentTypeFieldsDropZoneComponent
     */
    isTab(row: FieldDivider): boolean {
        return row instanceof FieldTab;
    }

    private setDroppedField(droppedField: ContentTypeField): void {
        this.formData = droppedField;

        if (this.formData) {
            this.currentFieldType = this.fieldPropertyService.getFieldType(this.formData.clazz);
        }
    }

    private setModel(data: { columnId: string; model: ContentTypeField[] }): void {
        const modelFieldColumn: FieldColumn = this.fieldRows
            .filter((fieldDivider: FieldDivider) => fieldDivider instanceof FieldRow)
            .map((fieldDivider: FieldDivider) => (<FieldRow>fieldDivider).columns)
            .reduce((acc, val) => acc.concat(val), [])
            .find((fieldColumn: FieldColumn) => fieldColumn.id === data.columnId);

        modelFieldColumn.fields = data.model;
    }

    private moveFields(): void {
        const fields = this.getFields().filter((field, index) => {
            const currentSortOrder = index + 1;

            if (field.sortOrder !== currentSortOrder) {
                field.sortOrder = currentSortOrder;
                return true;
            } else {
                return false;
            }
        });

        this.saveFields.emit(fields);
    }

    private getFieldsToSave(fieldToSave: ContentTypeField): ContentTypeField[] {
        return this.formData.id
            ? [this.getUpdatedField(fieldToSave)]
            : this.getNewFields(fieldToSave);
    }

    private getUpdatedField(fieldToSave: ContentTypeField): ContentTypeField {
        const fields = this.getFields();
        let result: ContentTypeField;

        for (let i = 0; i < fields.length; i++) {
            const field = fields[i];

            if (this.formData.id === field.id) {
                result = Object.assign({}, field, fieldToSave);
                break;
            }
        }

        return result;
    }

    private getNewFields(fieldToSave: ContentTypeField): ContentTypeField[] {
        const fields = this.getFields();
        const result: ContentTypeField[] = [];

        fields.forEach((field, index) => {
            if (FieldUtil.isNewField(field)) {
                field.sortOrder = index + 1;
                const fieldToPush = FieldUtil.isRowOrColumn(field)
                    ? field
                    : Object.assign(field, fieldToSave);
                result.push(fieldToPush);
            }
        });

        return result;
    }

    private toggleDialog(): void {
        this.displayDialog = !this.displayDialog;
        this.dialogActiveTab = 0;

        if (!this.displayDialog) {
            this.propertiesForm.destroy();
        }
    }

    private getFields(): ContentTypeField[] {
        const fields: ContentTypeField[] = [];

        this.fieldRows.forEach((fieldDivider: FieldDivider) => {
            const divider: ContentTypeField = fieldDivider.getFieldDivider();

            fields.push(divider);

            if (fieldDivider instanceof FieldRow) {
                (<FieldRow>fieldDivider).columns.forEach((fieldColumn) => {
                    fields.push(fieldColumn.columnDivider);
                    fieldColumn.fields.forEach((field) => fields.push(field));
                });
            }
        });

        return fields;
    }

    private getRowFields(fields: ContentTypeField[]): FieldDivider[] {
        const splitFields: ContentTypeField[][] = FieldUtil.splitFieldsByRows(fields);
        const fieldRows: FieldDivider[] = [];

        splitFields.forEach((fieldsInRow: ContentTypeField[]) => {
            if (FieldUtil.isTabDivider(fieldsInRow[0])) {
                const tabRow: FieldTab = new FieldTab(fieldsInRow[0]);
                fieldRows.push(tabRow);

                if (fieldsInRow.length > 1) {
                    fieldRows.push(this.generateRow(fieldsInRow.slice(1)));
                }
            } else {
                fieldRows.push(this.generateRow(fieldsInRow));
            }
        });

        return fieldRows.length ? fieldRows : this.getEmptyRow();
    }

    private generateRow(fieldDivider: ContentTypeField[]): FieldRow {
        const fieldRow: FieldRow = new FieldRow();
        fieldRow.addFields(JSON.parse(JSON.stringify(fieldDivider)));
        return fieldRow;
    }

    private getEmptyRow(): FieldDivider[] {
        const row = new FieldRow(1);

        return [row];
    }
}
