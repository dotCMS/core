import { UntypedFormControl } from '@angular/forms';

/**
 * Check if a valur has only white space
 *
 * @export
 * @param {FormControl} formControl
 * @returns
 */
export function noWhitespaceValidator(control: UntypedFormControl) {
    const isWhitespace = (control.value || '').trim().length === 0;
    const isValid = !isWhitespace;
    return isValid
        ? null
        : {
              valid: false
          };
}
