import { Component, Input, ViewChild } from '@angular/core';
import { BaseComponent } from '../../../../../../view/components/_common/_base/base-component';
import { MessageService } from '../../../../../../api/services/messages-service';
import { FieldProperty } from '../field-properties.model';
import { FormGroup } from '@angular/forms';
import { DotTextareaContentComponent } from '../../../../../../view/components/_common/dot-textarea-content/dot-textarea-content.component';

@Component({
    selector: 'values-property',
    templateUrl: './values-property.component.html',
})
export class ValuesPropertyComponent extends BaseComponent {
    @ViewChild('value') value: DotTextareaContentComponent;
    property: FieldProperty;
    group: FormGroup;

    constructor(public messageService: MessageService) {
        super(
            [
                'contenttypes.field.properties.value.label',
            ],
            messageService
        );
    }
}
