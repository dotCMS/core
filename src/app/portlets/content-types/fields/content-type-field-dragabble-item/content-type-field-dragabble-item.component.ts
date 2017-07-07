import { Component, Input, Output, EventEmitter } from '@angular/core';
import { Field } from '../service/field-types.service';

/**
 * It is a Field drop into the Content types drop zone
 *
 * @export
 * @class ContentTypesFieldDragabbleItemComponent
 */
@Component({
    selector: 'content-type-field-dragabble-item',
    templateUrl: './content-type-field-dragabble-item.component.html',
})
export class ContentTypesFieldDragabbleItemComponent {
    @Input() field: Field;
    @Output() remove: EventEmitter<Field> = new EventEmitter();

    removeField(): void {
        this.remove.emit(this.field);
    }
}