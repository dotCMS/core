import { Component, Input, Output, EventEmitter } from '@angular/core';

import { Field, FieldRow } from '../shared';
import { BaseComponent } from '../../../../view/components/_common/_base/base-component';
import { MessageService } from '../../../../api/services/messages-service';
import { ConfirmationService } from 'primeng/primeng';

/**
 * Display all the Field Types
 *
 * @export
 * @class FieldTypesContainerComponent
 */
@Component({
    selector: 'content-type-fields-row',
    styleUrls: ['./content-type-fields-row.component.scss'],
    templateUrl: './content-type-fields-row.component.html',
})
export class ContentTypeFieldsRowComponent extends BaseComponent {
    @Input() fieldRow: FieldRow;

    @Output() editField: EventEmitter<Field> = new EventEmitter();
    @Output() removeField: EventEmitter<Field> = new EventEmitter();
    @Output() removeRow: EventEmitter<FieldRow> = new EventEmitter();

    constructor(messageService: MessageService, private confirmationService: ConfirmationService) {
        super(
            [
                'contenttypes.dropzone.rows.empty.message',
                'contenttypes.action.delete',
                'message.structure.delete.structure',
                'message.structure.delete.content',
                'message.structure.delete.notice',
                'contenttypes.action.yes',
                'contenttypes.action.no'
            ],
            messageService
        );
    }

    /**
     * Remove a field
     * @param field field to remove
     */
    onRemoveField(field: Field): void {
        this.confirmationService.confirm({
            accept: () => {
                this.fieldRow.columns = this.fieldRow.columns.map(col => {
                    const index: number = col.fields.indexOf(field);
                    if (index !== -1) {
                        col.fields.splice(index, 1);
                    }
                    return col;
                });
                this.removeField.emit(field);
            },
            header: `${this.i18nMessages['contenttypes.action.delete']} Field`,
            message: `${this.i18nMessages['message.structure.delete.structure']} field named '${field.name}'? ${this.i18nMessages['message.structure.delete.notice']}`
        });
    }

    /**
     * Return the width for each column
     * @returns {string} Return the column's width width '%', for example, '30%'
     * @memberof ContentTypeFieldsRowComponent
     */
    getColumnWidth(): string {
        const nColumns = this.fieldRow.columns.length;
        return `${100 / nColumns}%`;
    }

    /**
     * Tigger the removeRow event whit the current FieldRow
     */
    onRemoveFieldRow(): void {
        this.confirmationService.confirm({
            accept: () => {
                this.removeRow.emit(this.fieldRow);
            },
            header: `${this.i18nMessages['contenttypes.action.delete']} Row`,
            message: `${this.i18nMessages['message.structure.delete.structure']} Row ${this.i18nMessages['message.structure.delete.content']} ${this.i18nMessages['message.structure.delete.notice']}`
        });
    }
}
