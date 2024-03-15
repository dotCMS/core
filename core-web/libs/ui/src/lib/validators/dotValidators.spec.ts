import { UntypedFormControl } from '@angular/forms';

import { DotValidators } from './dotValidators';

describe('DotValidators', () => {
    describe('alphaNumeric', () => {
        it('should return hasError false when input value is only alphanumeric', () => {
            const control = new UntypedFormControl('input', DotValidators.alphaNumeric);
            control.setValue('asdfgasdasdh');

            expect(control.hasError('alphaNumeric')).toBe(false);
        });

        it('should return hasError true when the input value has an hyphen', () => {
            const control = new UntypedFormControl('input', DotValidators.alphaNumeric);
            control.setValue('asdfga-sdasdh');

            expect(control.hasError('alphaNumeric')).toBe(true);
        });

        it('should return hasError true when the input value has an space', () => {
            const control = new UntypedFormControl('input', DotValidators.alphaNumeric);
            control.setValue('asdfga sdasdh');

            expect(control.hasError('alphaNumeric')).toBe(true);
        });
    });

    describe('validQueryParamName', () => {
        it('should return hasError false when input value is only validQueryParamName', () => {
            const control = new UntypedFormControl('input', DotValidators.validQueryParamName);
            control.setValue('myQueryParamName');

            expect(control.hasError('validQueryParamName')).toBe(false);
        });

        it('should return hasError false when the input value has a hyphen', () => {
            const control = new UntypedFormControl('input', DotValidators.validQueryParamName);
            control.setValue('my-amazing-parametername');

            expect(control.hasError('validQueryParamName')).toBe(false);
        });

        it('should return hasError false when the input value has a underscore', () => {
            const control = new UntypedFormControl('input', DotValidators.validQueryParamName);
            control.setValue('my_amazing_parametername');

            expect(control.hasError('validQueryParamName')).toBe(false);
        });

        it('should return hasError true when the input value has a space', () => {
            const control = new UntypedFormControl('input', DotValidators.validQueryParamName);
            control.setValue('my amazing parametername');

            expect(control.hasError('validQueryParamName')).toBe(true);
        });

        it('should return hasError true when the input value has a special character', () => {
            const control = new UntypedFormControl('input', DotValidators.validQueryParamName);

            control.setValue('my+amazing+parametername');
            expect(control.hasError('validQueryParamName')).toBe(true);

            control.setValue('my=amazing=parametername');
            expect(control.hasError('validQueryParamName')).toBe(true);

            control.setValue('my%amazing%parametername');
            expect(control.hasError('validQueryParamName')).toBe(true);
        });

        it('should return hasError true when the input is all white spaces', () => {
            const control = new UntypedFormControl('input', DotValidators.noWhitespace);
            control.setValue('       ');
            expect(control.hasError('whiteSpaceOnly')).toBe(true);
        });

        it('should return not return error when the input contain text', () => {
            const control = new UntypedFormControl('input', DotValidators.noWhitespace);
            control.setValue('  test     ');
            expect(control.hasError('whiteSpaceOnly')).toBe(false);
        });
    });
});
