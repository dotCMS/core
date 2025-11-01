import {
    dateRangeValidator,
    dateTimeValidator,
    dateValidator,
    numberValidator,
    regexValidator,
    stringValidator,
    timeValidator
} from './props';

import { PropValidationInfo } from '../models';

describe('Props Validators', () => {
    let propInfo: PropValidationInfo<any>;

    beforeEach(async () => {
        propInfo = {
            field: { type: 'test-type', name: 'field-name' },
            name: 'test-name',
            value: 'test-value'
        };
    });

    describe('stringValidator', () => {
        it('should not console.warn message when value is a string', () => {
            expect(() => stringValidator(propInfo)).not.toThrowError();
        });

        it('should console.warn message when value is not a string', () => {
            propInfo.value = {};
            expect(() => stringValidator(propInfo)).toThrowError();
        });
    });

    describe('regexValidator', () => {
        it('should not console.warn message when regular expression is valid', () => {
            propInfo.value = '[0-9]';
            expect(() => regexValidator(propInfo)).not.toThrowError();
        });

        it('should console.warn message when when regular expression is invalid', () => {
            propInfo.value = '[*';
            expect(() => regexValidator(propInfo)).toThrowError();
        });
    });

    describe('numberValidator', () => {
        it('should not  console.warn message when is a number', () => {
            propInfo.value = 123;
            expect(() => numberValidator(propInfo)).not.toThrowError();
        });

        it('should console.warn message when when is not a number', () => {
            expect(() => numberValidator(propInfo)).toThrowError();
        });
    });

    describe('dateValidator', () => {
        it('should not console.warn message when is a valid date', () => {
            propInfo.value = '2010-10-10';
            expect(() => dateValidator(propInfo)).not.toThrowError();
        });

        it('should console.warn message when when is a invalid date', () => {
            expect(() => dateValidator(propInfo)).toThrowError();
        });
    });

    describe('dateRangeValidator', () => {
        it('should not console.warn message when dates are valid', () => {
            propInfo.value = '2010-10-10,2010-11-11';
            expect(() => dateRangeValidator(propInfo)).not.toThrowError();
        });

        it('should console.warn message when when second date is higher than first', () => {
            propInfo.value = '2010-11-12,2010-10-10';
            expect(() => dateRangeValidator(propInfo)).toThrowError();
        });

        it('should console.warn message when when fist date is not valid', () => {
            propInfo.value = 'A2010-10-10,2010-11-12';
            expect(() => dateRangeValidator(propInfo)).toThrowError();
        });

        it('should console.warn message when when second  date is not valid', () => {
            propInfo.value = '2010-10-10,B2010-11-12';
            expect(() => dateRangeValidator(propInfo)).toThrowError();
        });

        it('should console.warn message when value are not dates', () => {
            expect(() => dateRangeValidator(propInfo)).toThrowError();
        });
    });

    describe('timeValidator', () => {
        it('should not console.warn message when is a valid time', () => {
            propInfo.value = '10:10:10';
            expect(() => timeValidator(propInfo)).not.toThrowError();
        });

        it('should console.warn message when when is a invalid time', () => {
            expect(() => timeValidator(propInfo)).toThrowError();
        });
    });

    describe('dateTimeValidator', () => {
        it('should not console.warn message when is a valid date and rime', () => {
            propInfo.value = '2010-10-10 10:10:10';
            expect(() => dateTimeValidator(propInfo)).not.toThrowError();
        });

        it('should not console.warn message when is a valid date', () => {
            propInfo.value = '2010-10-10';
            expect(() => dateTimeValidator(propInfo)).not.toThrowError();
        });

        it('should not console.warn message when is a valid time', () => {
            propInfo.value = '10:10:10';
            expect(() => dateTimeValidator(propInfo)).not.toThrowError();
        });

        it('should console.warn message only when only date is invalid', () => {
            propInfo.value = '2010-99-10 10:10:10';
            expect(() => dateTimeValidator(propInfo)).toThrowError();
        });

        it('should console.warn message when date is invalid', () => {
            propInfo.value = '2010-99-10';
            expect(() => dateTimeValidator(propInfo)).toThrowError();
        });

        it('should console.warn message only when time is invalid', () => {
            propInfo.value = '2010-99-10 1:10:10';
            expect(() => dateTimeValidator(propInfo)).toThrowError();
        });

        it('should console.warn message when time is invalid', () => {
            propInfo.value = '1:10:10';
            expect(() => dateTimeValidator(propInfo)).toThrowError();
        });

        it('should console.warn message when value is invalid', () => {
            expect(() => dateTimeValidator(propInfo)).toThrowError();
        });
    });
});
