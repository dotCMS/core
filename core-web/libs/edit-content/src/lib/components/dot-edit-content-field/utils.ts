import { Type } from '@angular/core';

import { DotEditContentRadioFieldComponent } from '../../fields/dot-edit-content-radio-field/dot-edit-content-radio-field.component';
import { DotEditContentSelectFieldComponent } from '../../fields/dot-edit-content-select-field/dot-edit-content-select-field.component';
import { DotEditContentTextAreaComponent } from '../../fields/dot-edit-content-text-area/dot-edit-content-text-area.component';
import { DotEditContentTextFieldComponent } from '../../fields/dot-edit-content-text-field/dot-edit-content-text-field.component';

// Map to match the field type to component selector
export enum FIELD_TYPES {
    TEXT = 'Text',
    TEXTAREA = 'Textarea',
    SELECT = 'Select',
    RADIO = 'Radio'
}

// This holds the mapping between the field type and the component that should be used to render it.
export const FIELD_TYPES_COMPONENTS: Record<FIELD_TYPES, Type<unknown>> = {
    // We had to use unknown because components have different types.
    [FIELD_TYPES.TEXT]: DotEditContentTextFieldComponent,
    [FIELD_TYPES.TEXTAREA]: DotEditContentTextAreaComponent,
    [FIELD_TYPES.SELECT]: DotEditContentSelectFieldComponent,
    [FIELD_TYPES.RADIO]: DotEditContentRadioFieldComponent
};
