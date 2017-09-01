import { Component, Input } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { BaseComponent } from '../../../../../../view/components/_common/_base/base-component';
import { MessageService } from '../../../../../../api/services/messages-service';
import { FieldProperty } from '../field-properties.model';

@Component({
    selector: 'hint-property',
    templateUrl: './hint-property.component.html',
})
export class HintPropertyComponent extends BaseComponent {
    property: FieldProperty;
    group: FormGroup;

    constructor(public messageService: MessageService) {
        super(['contenttypes.field.properties.hint.label'], messageService);
    }
}
