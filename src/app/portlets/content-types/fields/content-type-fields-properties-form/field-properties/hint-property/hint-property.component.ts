import { Component } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { BaseComponent } from '@components/_common/_base/base-component';
import { DotMessageService } from '@services/dot-messages-service';
import { FieldProperty } from '../field-properties.model';

@Component({
    selector: 'dot-hint-property',
    templateUrl: './hint-property.component.html'
})
export class HintPropertyComponent extends BaseComponent {
    property: FieldProperty;
    group: FormGroup;

    constructor(public dotMessageService: DotMessageService) {
        super(['contenttypes.field.properties.hint.label'], dotMessageService);
    }
}
