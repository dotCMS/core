import { AbstractControl, ValidationErrors } from '@angular/forms';

const QUERY_PARAM_NAME_REGEX = /^[a-zA-Z0-9\-_]+$/;
const ALPHA_NUMERIC_REGEX = /^[a-zA-Z0-9_]*$/;
const URL_REGEX = /^(ftp|http|https):\/\/[^ "]+$/;

const DOT_ERROR_MESSAGES = {
    alphaNumericErrorMsg: {
        alphaNumeric: 'contenttypes.form.field.validation.alphanumeric'
    },
    validQueryParamNameErrorMsg: {
        validQueryParamName: 'dot.common.form.field.validation.validQueryParamName'
    },
    whiteSpaceOnlyMgs: {
        whiteSpaceOnly: 'dot.common.form.field.validation.noWhitespace'
    },
    urlMsg: {
        invalidUrl: 'dot.common.form.field.validation.url'
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
     * Validates a string to allow empty input while ensuring it does not consist solely of white space characters.
     *
     * @param {AbstractControl} control
     */
    static noWhitespace(control: AbstractControl): { [key: string]: string } | null {
        const notOnlySpacesPattern = /^(?!\s+$).*/;

        return notOnlySpacesPattern.test(control.value)
            ? null
            : DOT_ERROR_MESSAGES.whiteSpaceOnlyMgs;
    }

    /**
     * Validate if the given control value is a valid URL
     *
     * @param {AbstractControl} control
     * @returns {ValidationErrors | null}
     */
    static url(control: AbstractControl): ValidationErrors | null {
        return URL_REGEX.test(control.value) ? null : DOT_ERROR_MESSAGES.urlMsg;
    }
}
