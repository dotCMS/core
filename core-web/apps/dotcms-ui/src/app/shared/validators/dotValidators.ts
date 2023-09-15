import { AbstractControl } from '@angular/forms';

const QUERY_PARAM_NAME_REGEX = /^[a-zA-Z0-9\-_]+$/;
const ALPHA_NUMERIC_REGEX = /^[a-zA-Z0-9_]*$/;

const DOT_ERROR_MESSAGES = {
    alphaNumericErrorMsg: {
        alphaNumeric: 'contenttypes.form.field.validation.alphanumeric'
    },
    validQueryParamNameErrorMsg: {
        validQueryParamName: 'dot.common.form.field.validation.validQueryParamName'
    },
    whiteSpaceOnlyMgs: {
        whiteSpaceOnly: 'dot.common.form.field.validation.noWhitespace'
    }
};

export class DotValidators {
    static alphaNumeric(control: AbstractControl): { [key: string]: string } | null {
        return ALPHA_NUMERIC_REGEX.test(control.value)
            ? null
            : DOT_ERROR_MESSAGES.alphaNumericErrorMsg;
    }

    /**
     * Validate if the query param name is valid
     * Rules: no space, only - and _ as special characters, and alphanumeric a-z, A-Z, 0-9
     *
     * @param {AbstractControl} control
     */
    static validQueryParamName(control: AbstractControl): { [key: string]: string } | null {
        return QUERY_PARAM_NAME_REGEX.test(control.value)
            ? null
            : DOT_ERROR_MESSAGES.validQueryParamNameErrorMsg;
    }

    /**
     * Validate there is not only white spaces in a field when is not empty.
     *
     * @param {AbstractControl} control
     */
    static noWhitespace(control: AbstractControl): { [key: string]: string } | null {
        return control.value
            ? control.value.trim().length
                ? null
                : DOT_ERROR_MESSAGES.whiteSpaceOnlyMgs
            : null;
    }
}
