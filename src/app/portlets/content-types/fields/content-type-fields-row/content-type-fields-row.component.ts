import { Component, Input, Output, EventEmitter, OnChanges, SimpleChanges } from '@angular/core';

import { ContentTypeField, FieldRow } from '../shared';
import { BaseComponent } from '../../../../view/components/_common/_base/base-component';
import { DotMessageService } from '../../../../api/services/dot-messages-service';
import { DotAlertConfirmService } from '../../../../api/services/dot-alert-confirm';
import { FieldColumn } from '..';

/**
 * Display all the Field Types
 *
 * @export
 * @class FieldTypesContainerComponent
 */
@Component({
    selector: 'dot-content-type-fields-row',
    styleUrls: ['./content-type-fields-row.component.scss'],
    templateUrl: './content-type-fields-row.component.html'
})
export class ContentTypeFieldsRowComponent extends BaseComponent implements OnChanges {
    @Input() fieldRow: FieldRow;

    @Output() editField: EventEmitter<ContentTypeField> = new EventEmitter();
    @Output() removeField: EventEmitter<ContentTypeField> = new EventEmitter();
    @Output() removeRow: EventEmitter<FieldRow> = new EventEmitter();

    showRemoveRow = true;

    constructor(dotMessageService: DotMessageService, private dotDialogService: DotAlertConfirmService) {
        super(
            [
                'contenttypes.dropzone.rows.empty.message',
                'contenttypes.action.delete',
                'contenttypes.confirm.message.delete.field',
                'contenttypes.confirm.message.delete.row',
                'contenttypes.content.field',
                'contenttypes.content.row',
                'contenttypes.action.cancel'
            ],
            dotMessageService
        );
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.fieldRow && changes.fieldRow.currentValue) {
            const cols: FieldColumn[] = changes.fieldRow.currentValue.columns;
            this.showRemoveRow = cols.every((col: FieldColumn) => col.fields.length === 0);
        }
    }

    /**
     * Remove a field
     * @param field field to remove
     */
    onRemoveField(field: ContentTypeField): void {
        this.dotDialogService.confirm({
            accept: () => {
                this.removeField.emit(field);
            },
            header: `${this.i18nMessages['contenttypes.action.delete']} ${this.i18nMessages['contenttypes.content.field']}`,
            message: this.dotMessageService.get('contenttypes.confirm.message.delete.field', field.name),
            footerLabel: {
                accept: this.i18nMessages['contenttypes.action.delete'],
                reject: this.i18nMessages['contenttypes.action.cancel']
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
        this.removeRow.emit(this.fieldRow);
    }

    private isRowFieldEmpty(): boolean {
        return this.fieldRow.columns.map((column: FieldColumn) => column.fields.length).every(fieldsNumber => fieldsNumber === 0);
    }
}
