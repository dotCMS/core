import { Component } from '@angular/core';
import { UntypedFormGroup } from '@angular/forms';

import { FieldProperty } from '../field-properties.model';

@Component({
    selector: 'dot-new-render-mode-property',
    templateUrl: './new-render-mode-property.component.html',
    standalone: false
})
export class NewRenderModePropertyComponent {
    property: FieldProperty;
    group: UntypedFormGroup;
}
