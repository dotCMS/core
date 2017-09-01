import * as moment from 'moment';

import { FormControl } from '@angular/forms';

const format = {
    'com.dotcms.contenttype.model.field.ImmutableDateField': 'YYYY-MM-DD',
    'com.dotcms.contenttype.model.field.ImmutableDateTimeField': 'YYYY-MM-DD HH:mm:ss',
    'com.dotcms.contenttype.model.field.ImmutableTimeField': 'HH:mm:ss'
};

/**
 * Validate defaultValue for date field, date_time Field and time field
 *
 * @export
 * @param {FormControl} formControl
 * @returns
 */
export function validateDateDefaultValue(formControl: FormControl) {
    const invalidResponse = {
            validateDate: {
            valid: false
        }
    };

    let valid = true;

    if (formControl.parent && formControl.value) {
        const clazz: string = formControl.parent.controls['clazz'].value;
        valid = format[clazz] ? moment(formControl.value, format[clazz], true).isValid() : true;
    }

    return valid ? null : invalidResponse;
}
