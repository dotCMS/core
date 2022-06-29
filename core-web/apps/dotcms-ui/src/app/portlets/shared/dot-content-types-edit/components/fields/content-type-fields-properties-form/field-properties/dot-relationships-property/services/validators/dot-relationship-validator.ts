import { FormControl } from '@angular/forms';

/**
 *Validate the values for a relationship property field are right.
 *
 * @export
 * @param {FormControl} formControl
 * @returns
 */
export function validateRelationship(formControl: FormControl) {
    if (formControl.value.cardinality !== undefined && formControl.value.velocityVar) {
        return null;
    } else {
        return {
            valid: false
        };
    }
}
