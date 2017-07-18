import { Component, Input } from '@angular/core';
import { Field, FieldRow } from '../';

/**
 * Display all the Field Types
 *
 * @export
 * @class FieldTypesContainerComponent
 */
@Component({
    selector: 'content-type-fields-row',
    styles: [require('./content-type-fields-row.component.scss')],
    templateUrl: './content-type-fields-row.component.html',
})
export class ContentTypeFieldsRowComponent {
    @Input() fieldRow: FieldRow;

    /**
     * Remove a field
     * @param field field to remove
     */
    removeField(field: Field): void {
        this.fieldRow.columns = this.fieldRow.columns.map(col => {
            let index: number = col.fields.indexOf(field);

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
        let nColumns = this.fieldRow.columns.length;
        return `${(100 - nColumns) / nColumns}%`;
    }
}