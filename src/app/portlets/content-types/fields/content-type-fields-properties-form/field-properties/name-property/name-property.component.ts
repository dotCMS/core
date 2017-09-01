import { Component, Input } from '@angular/core';
import { FormGroup } from '@angular/forms/forms';
import { FieldProperty } from '../field-properties.model';
import { MessageService } from '../../../../../../api/services/messages-service';
import { BaseComponent } from '../../../../../../view/components/_common/_base/base-component';

@Component({
    selector: 'name-property',
    templateUrl: './name-property.component.html',
})

export class NamePropertyComponent extends BaseComponent {
    property: FieldProperty;
    group: FormGroup;

    constructor(public messageService: MessageService) {
        super(
            [
                'contenttypes.field.properties.name.label',
                'contenttypes.field.properties.name.error.required'
            ],
            messageService
        );
    }
}
