import { Component, Input, Output, EventEmitter, HostListener } from '@angular/core';

import { BaseComponent } from '../../../../view/components/_common/_base/base-component';
import { ContentTypeField } from '../shared';
import { DotMessageService } from '../../../../api/services/dot-messages-service';
import { FieldService } from '../service';

/**
 * This display field after being dropped into a Content Type Drop zone
 * @export
 * @class ContentTypesFieldDragabbleItemComponent
 */
@Component({
    selector: 'dot-content-type-field-dragabble-item',
    styleUrls: ['./content-type-field-dragabble-item.component.scss'],
    templateUrl: './content-type-field-dragabble-item.component.html'
})
export class ContentTypesFieldDragabbleItemComponent extends BaseComponent {
    @Input() field: ContentTypeField;
    @Output() remove: EventEmitter<ContentTypeField> = new EventEmitter();
    @Output() edit: EventEmitter<ContentTypeField> = new EventEmitter();

    constructor(dotMessageService: DotMessageService, public fieldService: FieldService) {
        super(['contenttypes.action.edit', 'contenttypes.action.delete'], dotMessageService);
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
