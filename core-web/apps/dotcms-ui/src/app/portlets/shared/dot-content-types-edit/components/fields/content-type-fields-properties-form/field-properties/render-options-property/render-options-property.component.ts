import { Component, input } from '@angular/core';
import { UntypedFormGroup } from '@angular/forms';

@Component({
    selector: 'dot-render-options-property',
    templateUrl: './render-options-property.component.html',
    standalone: false
})
export class RenderOptionsPropertyComponent {
    /** Form group containing showAsModal, customFieldWidth, customFieldHeight */
    readonly group = input.required<UntypedFormGroup>();
}
