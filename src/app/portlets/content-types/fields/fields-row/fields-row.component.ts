import { Component } from '@angular/core';
import { Field } from '../service';

/**
 * Show all the Field Types
 *
 * @export
 * @class FieldTypesContainerComponent
 */
@Component({
    selector: 'fields-row',
    styles: [require('./fields-row.component.scss')],
    templateUrl: './fields-row.component.html',
})
export class FieldsRowComponent {
    fields: Field[] = [];

    removeField(field: Field): void {
        let index: number = this.fields.indexOf(field);
        this.fields.splice(index, 1);
    }
}