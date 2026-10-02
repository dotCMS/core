import { AbstractControl, ValidationErrors } from '@angular/forms';

import { _isValid } from '@dotcms/data-access';

/** Date formats by field clazz, which is whatever the sibling control holds. */
const format: Record<string, string> = {
    'com.dotcms.contenttype.model.field.ImmutableDateField': 'yyyy-MM-dd',
    'com.dotcms.contenttype.model.field.ImmutableDateTimeField': 'yyyy-MM-dd HH:mm:ss',
    'com.dotcms.contenttype.model.field.ImmutableTimeField': 'HH:mm:ss'
};

/**
 * Validate defaultValue for date field, date_time Field and time field
 *
 * @export
 * @param FormControl formControl
 * @returns
 */
export function validateDateDefaultValue(formControl: AbstractControl): ValidationErrors | null {
    const invalidResponse = {
        validateDate: {
            valid: false
        }
    };

    let valid = true;

    if (formControl.parent && formControl.value) {
        valid = isValueValid(formControl);
    }

    return valid ? null : invalidResponse;
}

function isValueValid(formControl: AbstractControl): boolean {
    const clazz: string = formControl.parent?.get('clazz')?.value;

    return format[clazz]
        ? _isValid(formControl.value, format[clazz]) || formControl.value === 'now'
        : true;
}
