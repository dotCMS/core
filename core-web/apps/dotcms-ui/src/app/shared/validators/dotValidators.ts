import { AbstractControl } from '@angular/forms';

const ALPHA_NUMERIC_REGEX = /^[a-zA-Z0-9_]*$/;
const ALPHA_NUMERIC_VALIDATION_ERROR = {
    alphaNumericError: 'contenttypes.form.field.validation.alphanumeric'
};

export class DotValidators {
    static alphaNumeric(control: AbstractControl): { [key: string]: string } | null {
        return ALPHA_NUMERIC_REGEX.test(control.value) ? null : ALPHA_NUMERIC_VALIDATION_ERROR;
    }
}
