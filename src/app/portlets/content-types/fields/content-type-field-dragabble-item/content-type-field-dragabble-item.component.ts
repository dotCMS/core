import { Component, Input, Output, EventEmitter, HostListener } from '@angular/core';

import { BaseComponent } from '../../../../view/components/_common/_base/base-component';
import { Field } from '../shared';
import { DotMessageService } from '../../../../api/services/dot-messages-service';
import { FieldService } from '../service';

/**
 * This display field after being dropped into a Content Type Drop zone
 * @export
 * @class ContentTypesFieldDragabbleItemComponent
 */
@Component({
    selector: 'content-type-field-dragabble-item',
    styleUrls: ['./content-type-field-dragabble-item.component.scss'],
    templateUrl: './content-type-field-dragabble-item.component.html'
})
export class ContentTypesFieldDragabbleItemComponent extends BaseComponent {
    @Input() field: Field;
    @Output() remove: EventEmitter<Field> = new EventEmitter();
    @Output() edit: EventEmitter<Field> = new EventEmitter();

    constructor(messageService: DotMessageService, public fieldService: FieldService) {
        super(['contenttypes.action.edit', 'contenttypes.action.delete'], messageService);
    }

    @HostListener('click', ['$event'])
    onClick($event: MouseEvent) {
        $event.stopPropagation();
        this.edit.emit(this.field);
    }

    removeItem($event: MouseEvent): void {
        this.remove.emit(this.field);
        $event.stopPropagation();
    }
}
