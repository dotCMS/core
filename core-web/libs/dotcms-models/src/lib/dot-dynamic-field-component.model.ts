import { FormGroup } from '@angular/forms';
import { DotCMSContentTypeField } from './dot-content-types.model';

export interface DotDynamicFieldComponent {
    property: DotDynamicFieldComponentProperty;
    group: FormGroup;
    helpText: string;
}

interface DotDynamicFieldComponentProperty {
    field: DotCMSContentTypeField;
    name: string;
    value: unknown;
}
