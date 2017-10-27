///<reference path="../shared/field-type.model.ts"/>
import { BaseComponent } from '../../../../view/components/_common/_base/base-component';
import { Component, SimpleChanges, Input, Output, EventEmitter, OnInit, OnChanges, ViewChild } from '@angular/core';
import { FieldDragDropService } from '../service';
import { FieldRow, Field, FieldType } from '../shared';
import { ContentTypeFieldsPropertiesFormComponent } from '../content-type-fields-properties-form';
import { MessageService } from '../../../../api/services/messages-service';
import { FieldUtil } from '../util/field-util';
import { FieldPropertyService } from '../service/field-properties.service';

/**
 * Display all the Field Types
 *
 * @export
 * @class FieldTypesContainerComponent
 */
@Component({
    selector: 'dot-content-type-fields-drop-zone',
    styleUrls: ['./content-type-fields-drop-zone.component.scss'],
    templateUrl: './content-type-fields-drop-zone.component.html'
})
export class ContentTypeFieldsDropZoneComponent extends BaseComponent implements OnInit, OnChanges {
    displayDialog = false;
    fieldRows: FieldRow[] = [];
    formData: Field;
    currentFieldType: FieldType;

    @ViewChild('fieldPropertiesForm') propertiesForm: ContentTypeFieldsPropertiesFormComponent;

    @Input() fields: Field[];
    @Output() saveFields = new EventEmitter<Field[]>();
    @Output() removeFields = new EventEmitter<Field[]>();

    constructor(
        messageService: MessageService,
        private fieldDragDropService: FieldDragDropService,
        private fieldPropertyService: FieldPropertyService
    ) {
        super(
            [
                'contenttypes.dropzone.action.save',
                'contenttypes.dropzone.action.cancel',
                'contenttypes.dropzone.action.edit',
                'contenttypes.dropzone.action.create.field',
                'contenttypes.dropzone.empty.message'
            ],
            messageService
        );
    }

    ngOnInit(): void {
        this.fieldDragDropService.fieldDropFromSource$.subscribe(() => {
            this.setDroppedField();
            this.toggleDialog();
        });

        this.fieldDragDropService.fieldDropFromTarget$.subscribe(() => {
            this.moveFields();
        });

        this.fieldDragDropService.fieldRowDropFromTarget$.subscribe(() => this.moveFields());
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

    /**
     * Emit the saveField event
     * @param {Field} fieldToSave
     * @memberof ContentTypeFieldsDropZoneComponent
     */
    saveFieldsHandler(fieldToSave: Field): void {
        const fields = this.getFieldsToSave(fieldToSave);
        this.saveFields.emit(fields);
        this.toggleDialog();
    }

    /**
     * Get the field to be edited
     * @param {Field} fieldToEdit
     * @memberof ContentTypeFieldsDropZoneComponent
     */
    editField(fieldToEdit: Field): void {
        const fields = this.getFields();
        this.formData = fields.filter(field => fieldToEdit.id === field.id)[0];
        this.currentFieldType = this.fieldPropertyService.getFieldType(this.formData.clazz);
        this.toggleDialog();
    }

    /**
     * Set the field to be edited
     * @memberof ContentTypeFieldsDropZoneComponent
     */
    setDroppedField(): void {
        const fields = this.getFields();
        this.formData = fields.find(field => FieldUtil.isNewField(field) && !FieldUtil.isRowOrColumn(field));
        if (this.formData) {
            this.currentFieldType = this.fieldPropertyService.getFieldType(this.formData.clazz);
        }
    }

    /**
     * Remove the last dropped field added without ID
     * @memberof ContentTypeFieldsDropZoneComponent
     */
    removeFieldsWithoutId(): void {
        const fieldRows: any = this.fieldRows;
        // TODO needs an improvement for performance reasons
        fieldRows.forEach(row => {
            row.columns.forEach((col, colIndex) => {
                col.fields.forEach((field, fieldIndex) => {
                    if (!field.id) {
                        row.columns[colIndex].fields.splice(fieldIndex, 1);
                    }
                });
            });
        });

        this.formData = null;
        this.propertiesForm.destroy();
    }

    // TODO: Remove if we will not use this anymore.
    getDialogHeader(): string {
        const dialogTitle =
            this.formData && this.formData.id
                ? this.i18nMessages['contenttypes.dropzone.action.edit']
                : this.i18nMessages['contenttypes.dropzone.action.create.field'];
        return `${dialogTitle}`;
    }

    /**
     * Tigger the removeFields event with fieldToDelete
     * @param fieldToDelete
     */
    removeField(fieldToDelete: Field): void {
        this.removeFields.emit([fieldToDelete]);
    }

    /**
     * Tigger the removeFields event with all the fields in fieldRow
     * @param fieldToDelete
     */
    removeFieldRow(fieldRow: FieldRow): void {
        this.fieldRows.splice(this.fieldRows.indexOf(fieldRow), 1);
        const fieldsToDelete: Field[] = [];

        fieldsToDelete.push(fieldRow.lineDivider);
        fieldRow.columns.forEach(fieldColumn => {
            fieldsToDelete.push(fieldColumn.tabDivider);
            fieldColumn.fields.forEach(field => fieldsToDelete.push(field));
        });
        this.removeFields.emit(fieldsToDelete);
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

    private getFieldsToSave(fieldToSave: Field): Field[] {
        return this.formData.id ? [this.getUpdatedField(fieldToSave)] : this.getNewFields(fieldToSave);
    }

    private getUpdatedField(fieldToSave: Field): Field {
        const fields = this.getFields();
        let result: Field;

        for (let i = 0; i < fields.length; i++) {
            const field = fields[i];

            if (this.formData.id === field.id) {
                result = Object.assign(field, fieldToSave);
                break;
            }
        }

        return result;
    }

    private getNewFields(fieldToSave: Field): Field[] {
        const fields = this.getFields();
        const result: Field[] = [];

        fields.forEach((field, index) => {
            if (FieldUtil.isNewField(field)) {
                field.sortOrder = index + 1;
                const fieldToPush = FieldUtil.isRowOrColumn(field) ? field : Object.assign(field, fieldToSave);
                result.push(fieldToPush);
            }
        });

        return result;
    }

    private toggleDialog(): void {
        this.displayDialog = !this.displayDialog;
    }

    private getRowFields(fields: Field[]): FieldRow[] {
        let fieldRows: FieldRow[] = [];
        const splitFields: Field[][] = FieldUtil.splitFieldsByLineDivider(fields);

        fieldRows = splitFields.map(fieldsByLineDivider => {
            const fieldRow: FieldRow = new FieldRow();
            fieldRow.addFields(fieldsByLineDivider);
            return fieldRow;
        });

        return fieldRows;
    }

    private getFields(): Field[] {
        const fields: Field[] = [];

        this.fieldRows.forEach((fieldRow, rowIndex) => {
            fields.push(fieldRow.lineDivider);

            fieldRow.columns.forEach((fieldColumn, colIndex) => {
                fields.push(fieldColumn.tabDivider);
                fieldColumn.fields.forEach(field => fields.push(field));
            });
        });

        return fields;
    }
}
