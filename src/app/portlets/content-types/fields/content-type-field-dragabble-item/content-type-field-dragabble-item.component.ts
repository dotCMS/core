import { Component, Input, Output, EventEmitter, HostListener, OnInit } from '@angular/core';

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
export class ContentTypesFieldDragabbleItemComponent extends BaseComponent implements OnInit {
    @Input() field: ContentTypeField;
    @Output() remove: EventEmitter<ContentTypeField> = new EventEmitter();
    @Output() edit: EventEmitter<ContentTypeField> = new EventEmitter();
    fieldAttributes: string;

    constructor(dotMessageService: DotMessageService, public fieldService: FieldService) {
        super(['contenttypes.action.edit', 'contenttypes.action.delete'], dotMessageService);
    }

    ngOnInit(): void {
        this.dotMessageService
            .getMessages([
                'contenttypes.field.atributes.required',
                'contenttypes.field.atributes.indexed',
                'contenttypes.field.atributes.listed'
            ])
            .subscribe(() => {
                this.fieldAttributes = [
                    {
                        name: this.dotMessageService.get('contenttypes.field.atributes.required'),
                        value: this.field.required
                    },
                    {
                        name: this.dotMessageService.get('contenttypes.field.atributes.indexed'),
                        value: this.field.indexed
                    },
                    {
                        name: this.dotMessageService.get('contenttypes.field.atributes.listed'),
                        value: this.field.listed
                    }
                ]
                    .filter((field) => field.value)
                    .map((field) => field.name)
                    .join('&nbsp;&nbsp;&#8226;&nbsp;&nbsp;');
            });
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
