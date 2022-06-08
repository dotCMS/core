import { FormControl } from '@angular/forms';

/**
 * Check if a valur has only white space
 *
 * @export
 * @param {FormControl} formControl
 * @returns
 */
export function noWhitespaceValidator(control: FormControl) {
    const isWhitespace = (control.value || '').trim().length === 0;
    const isValid = !isWhitespace;
    return isValid
        ? null
        : {
              valid: false
          };
}
