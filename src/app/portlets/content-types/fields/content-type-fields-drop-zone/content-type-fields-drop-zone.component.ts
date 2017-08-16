import { BaseComponent } from '../../../../view/components/_common/_base/base-component';
import { Component, SimpleChanges, Input, Output, EventEmitter } from '@angular/core';
import { FieldService, FieldDragDropService } from '../service';
import { FieldRow, Field, FieldColumn, TAB_DIVIDER, LINE_DIVIDER } from '../shared';
import { ContentTypeFieldsPropertiesFormComponent } from '../content-type-fields-properties-form/index';
import { MessageService } from '../../../../api/services/messages-service';

/**
 * Display all the Field Types
 *
 * @export
 * @class FieldTypesContainerComponent
 */
@Component({
    selector: 'content-type-fields-drop-zone',
    styleUrls: ['./content-type-fields-drop-zone.component.scss'],
    templateUrl: './content-type-fields-drop-zone.component.html',
})
export class ContentTypeFieldsDropZoneComponent extends BaseComponent {
    displayDialog = false;
    fieldRows: FieldRow[] = [];
    formData: Field;
    @Input() fields: Field[];
    @Output('saveFields') saveFieldsEvent = new EventEmitter<Field[]>();

    constructor(private fieldDragDropService: FieldDragDropService, messageService: MessageService) {
        super(
            [
                'Save',
                'Cancel',
                'edit',
                'Create-field'
            ],
            messageService
        );
    }

    ngOnInit(): void {
        this.fieldDragDropService.fieldDrop$.subscribe((data) => {
            let dragType = data[0];

            if (dragType === 'fields-bag') {
                this.setDroppedField();
                this.toggleDialog();
            }
        });
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.fields.currentValue) {
            let fields = changes.fields.currentValue;

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
    saveFields(fieldToSave: Field): void {
        const fields = this.getFields();
        // Needs a better implementation
        fields.map(field => {
            if (this.isNewField(field)) {
                field = Object.assign(field, fieldToSave);
            } else if (field.id === this.formData.id) {
                field = Object.assign(field, fieldToSave);
            }

            return field;
        });

        this.saveFieldsEvent.emit(fields);
        this.toggleDialog();
    }

    /**
     * Get the field to be edited
     * @param {Field} fieldToEdit
     * @memberof ContentTypeFieldsDropZoneComponent
     */
    editField(fieldToEdit: Field): void {
        const fields = this.getFields();
        // Needs a better implementation
        fields.forEach((field) => {
            if (fieldToEdit.id === field.id) {
                this.formData = fieldToEdit;
            }
        });

        this.toggleDialog();
    }

    /**
     * Set the field to be edited
     * @memberof ContentTypeFieldsDropZoneComponent
     */
    setDroppedField(): void {
        const fields = this.getFields();
        // Needs a better implementation
        fields.forEach(field => {
            if (this.isNewField(field)) {
                this.formData = field;
            }
        });
    }

    /**
     * Show or hide dialog
     * @memberof ContentTypeFieldsDropZoneComponent
     */
    toggleDialog(): void {
        this.displayDialog = !this.displayDialog;
    }

    /**
     * Remove the last dropped field added without ID
     * @memberof ContentTypeFieldsDropZoneComponent
     */
    removeFieldsWithoutId(): void {
        const fieldRows: any = this.fieldRows;
        // TODO needs an improvement for performance reasons
        fieldRows.forEach((row) => {
            row.columns.forEach((col, colIndex) => {
                col.fields.forEach((field, fieldIndex) => {
                    if (!field.id) {
                        row.columns[colIndex].fields.splice(fieldIndex, 1);
                    }
                });
            });
        });

        this.toggleDialog();
    }

    /**
     * Verify if the Field already exist
     * @param {Field} field
     * @returns {Boolean}
     * @memberof ContentTypeFieldsDropZoneComponent
     */
    isNewField(field: Field): Boolean {
        return !field.id && !(this.isRow(field) || this.isColumn(field));
    }

    /**
     * Verify if the Field is a row
     * @param {Field} field
     * @returns {Boolean}
     * @memberof ContentTypeFieldsDropZoneComponent
     */
    isRow(field: Field): Boolean {
        return field.clazz === LINE_DIVIDER.clazz ? true : false;
    }

    /**
     * Verify if the Field is a column
     * @param {Field} field
     * @returns {Boolean}
     * @memberof ContentTypeFieldsDropZoneComponent
     */
    isColumn(field: Field): Boolean {
        return field.clazz === TAB_DIVIDER.clazz ? true : false;
    }

    private splitFieldsByLineDiveder(fields: Field[]): Field[][] {
        const result: Field[][] = [];
        let currentFields: Field[];
        fields.forEach(field => {
            if (field.clazz === LINE_DIVIDER.clazz) {
                currentFields = [];
                result.push(currentFields);
            }
            currentFields.push(field);
        });

        return result;
    }

    private getRowFields(fields: Field[]): FieldRow[] {
        let fieldRows: FieldRow[] = [];
        let splitFields: Field[][] = this.splitFieldsByLineDiveder(fields);

        fieldRows = splitFields.map(fields => {
            const fieldRow: FieldRow = new FieldRow();
            fieldRow.addFields(fields);
            return fieldRow;
        });

        return fieldRows;
    }

    private getFields(): Field[] {

        const fields: Field[] = [];

        this.fieldRows.forEach((fieldRow, rowIndex) => {
            fields.push(fieldRow.lineDivider);

            fieldRow.columns.forEach( (fieldColumn, colIndex) => {
                fields.push(fieldColumn.tabDivider);

                fieldColumn.fields.forEach( field => fields.push(field));
            });
        });

        return fields;
    }
}
