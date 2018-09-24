import { Component, ViewChild } from '@angular/core';
import { BaseComponent } from '@components/_common/_base/base-component';
import { DotMessageService } from '@services/dot-messages-service';
import { FieldProperty } from '../field-properties.model';
import { FormGroup } from '@angular/forms';
import { DotTextareaContentComponent } from '@components/_common/dot-textarea-content/dot-textarea-content.component';

@Component({
    selector: 'dot-values-property',
    templateUrl: './values-property.component.html'
})
export class ValuesPropertyComponent extends BaseComponent {
    @ViewChild('value')
    value: DotTextareaContentComponent;
    property: FieldProperty;
    group: FormGroup;

    constructor(public dotMessageService: DotMessageService) {
        super(['contenttypes.field.properties.value.label'], dotMessageService);
    }
}
