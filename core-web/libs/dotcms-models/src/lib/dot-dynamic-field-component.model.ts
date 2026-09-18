import { UntypedFormGroup } from '@angular/forms';

import { DotCMSContentTypeField } from './dot-content-types.model';

export interface DotDynamicFieldComponent {
    property: DotDynamicFieldComponentProperty;
    group: UntypedFormGroup;
    /**
     * Optional: only one of the ten field-property components surfaces help text, and the host
     * directive sets it unconditionally on whichever it renders.
     */
    helpText?: string;
}

interface DotDynamicFieldComponentProperty {
    field: DotCMSContentTypeField;
    name: string;
    value: unknown;
}
