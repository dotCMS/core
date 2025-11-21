import { Component } from '@angular/core';
import { UntypedFormGroup } from '@angular/forms';

import { FieldProperty } from '../field-properties.model';

@Component({
    selector: 'dot-new-render-mode-proptery',
    templateUrl: './new-render-mode-proptery.component.html',
    standalone: false
})
export class NewRenderModePropteryComponent {
    property: FieldProperty;
    group: UntypedFormGroup;
}
