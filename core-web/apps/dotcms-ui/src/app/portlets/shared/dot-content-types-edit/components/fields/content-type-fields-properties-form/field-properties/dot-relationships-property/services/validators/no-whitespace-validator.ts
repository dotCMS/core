import { AbstractControl, ValidationErrors } from '@angular/forms';

/**
 * Check if a valur has only white space
 *
 * @export
 * @param {FormControl} formControl
 * @returns
 */
export function noWhitespaceValidator(control: AbstractControl): ValidationErrors | null {
    const isWhitespace = (control.value || '').trim().length === 0;
    const isValid = !isWhitespace;

    return isValid
        ? null
        : {
              valid: false
          };
}
