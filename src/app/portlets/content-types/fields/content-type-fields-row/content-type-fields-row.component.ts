import { Component, Input, Output, EventEmitter } from '@angular/core';

import { Field, FieldRow } from '../shared';
import { BaseComponent } from '../../../../view/components/_common/_base/base-component';
import { MessageService } from '../../../../api/services/messages-service';

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

    constructor(messageService: MessageService) {
        super(
            [
                'contenttypes.dropzone.rows.empty.message',
                'contenttypes.action.delete'
            ],
            messageService
        );
    }

    /**
     * Remove a field
     * @param field field to remove
     */
    removeField(field: Field): void {
        this.fieldRow.columns = this.fieldRow.columns.map(col => {
            const index: number = col.fields.indexOf(field);

            if (index !== -1) {
                col.fields.splice(index, 1);
             }
            return col;
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
}
