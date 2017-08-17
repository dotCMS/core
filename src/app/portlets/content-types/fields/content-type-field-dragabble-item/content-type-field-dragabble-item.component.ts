import { Component, Input, Output, EventEmitter } from '@angular/core';

import { BaseComponent } from '../../../../view/components/_common/_base/base-component';
import { Field } from '../shared';
import { MessageService } from '../../../../api/services/messages-service';

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

    constructor(messageService: MessageService) {
        super(['contenttypes.action.edit', 'contenttypes.action.delete'], messageService);
    }
}
