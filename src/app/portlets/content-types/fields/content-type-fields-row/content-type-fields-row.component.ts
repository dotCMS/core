import { Component, Input, Output, EventEmitter } from '@angular/core';

import { Field, FieldRow } from '../shared';
import { BaseComponent } from '../../../../view/components/_common/_base/base-component';
import { DotMessageService } from '../../../../api/services/dot-messages-service';
import { DotConfirmationService } from '../../../../api/services/dot-confirmation';

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

    constructor(messageService: DotMessageService, private dotConfirmationService: DotConfirmationService) {
        super(
            [
                'contenttypes.dropzone.rows.empty.message',
                'contenttypes.action.delete',
                'contenttypes.confirm.message.delete',
                'contenttypes.confirm.message.delete.content',
                'contenttypes.confirm.message.delete.warning',
                'contenttypes.content.field',
                'contenttypes.content.row',
                'contenttypes.action.cancel'
            ],
            messageService
        );
    }

    /**
     * Remove a field
     * @param field field to remove
     */
    onRemoveField(field: Field): void {
        this.dotConfirmationService.confirm({
            accept: () => {
                this.getField(field);
                this.removeField.emit(field);
            },
            header: `${this.i18nMessages['contenttypes.action.delete']} ${this.i18nMessages['contenttypes.content.field']}`,
            message: `${this.i18nMessages['contenttypes.confirm.message.delete']} ${this.i18nMessages['contenttypes.content.field']}
                        '${field.name}'?
                        <span>${this.i18nMessages['contenttypes.confirm.message.delete.warning']}</span>`,
            footerLabel: {
                acceptLabel: this.i18nMessages['contenttypes.action.delete'],
                rejectLabel: this.i18nMessages['contenttypes.action.cancel']
            }
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
        this.dotConfirmationService.confirm({
            accept: () => {
                this.removeRow.emit(this.fieldRow);
            },
            header: `${this.i18nMessages['contenttypes.action.delete']} ${this.i18nMessages['contenttypes.content.row']}`,
            message: `${this.i18nMessages['contenttypes.confirm.message.delete']} ${this.i18nMessages['contenttypes.content.row']}
                        ${this.i18nMessages['contenttypes.confirm.message.delete.content']}
                        <span>${this.i18nMessages['contenttypes.confirm.message.delete.warning']}</span>`,
            footerLabel: {
                acceptLabel: this.i18nMessages['contenttypes.action.delete'],
                rejectLabel: this.i18nMessages['contenttypes.action.cancel']
            }
        });
    }

    private getField(field: Field): any {
        this.fieldRow.columns = this.fieldRow.columns.map(col => {
            const index: number = col.fields.indexOf(field);
            if (index !== -1) {
                col.fields.splice(index, 1);
            }
            return col;
        });
    }
}
