import { Component, Input } from '@angular/core';
import { FieldProperty } from '../field-properties.model';
import { BaseComponent } from '../../../../../../view/components/_common/_base/base-component';
import { DotMessageService } from '../../../../../../api/services/dot-messages-service';
import { FormGroup } from '@angular/forms';

@Component({
    selector: 'default-value-property',
    templateUrl: './default-value-property.component.html',
})
export class DefaultValuePropertyComponent extends BaseComponent {
    property: FieldProperty;
    group: FormGroup;

    constructor(public messageService: DotMessageService) {
        super(['contenttypes.field.properties.default_value.label'], messageService);
    }
}
