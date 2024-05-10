import { UntypedFormGroup } from '@angular/forms';

import { DotCMSContentTypeField } from './dot-content-types.model';

export interface DotDynamicFieldComponent {
    property: DotDynamicFieldComponentProperty;
    group: UntypedFormGroup;
    helpText: string;
}

interface DotDynamicFieldComponentProperty {
    field: DotCMSContentTypeField;
    name: string;
    value: unknown;
}
