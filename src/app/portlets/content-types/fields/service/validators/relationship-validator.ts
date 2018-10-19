import { FormControl } from '@angular/forms';

/**
 * Validate defaultValue for date field, date_time Field and time field
 *
 * @export
 * @param FormControl formControl
 * @returns
 */
export function validateRelationship(formControl: FormControl) {
    if (formControl.value.cardinality && formControl.value.velocityVar) {
        return null;
    } else {
        return {
            valid: true
        };
    }
}

