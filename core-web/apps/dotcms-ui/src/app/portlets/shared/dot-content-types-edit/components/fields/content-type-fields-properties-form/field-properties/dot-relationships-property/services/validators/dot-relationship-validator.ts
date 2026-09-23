import { AbstractControl, ValidationErrors } from '@angular/forms';

/**
 *Validate the values for a relationship property field are right.
 *
 * @export
 * @param {AbstractControl} formControl
 * @returns
 */
export function validateRelationship(formControl: AbstractControl): ValidationErrors | null {
    if (formControl.value.cardinality !== undefined && formControl.value.velocityVar) {
        return null;
    } else {
        return {
            valid: false
        };
    }
}
