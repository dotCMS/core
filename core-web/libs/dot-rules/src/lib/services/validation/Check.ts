import { Verify } from './Verify';

const createCheckError = function (validation, value, message): Error {
    const e = new Error('Check.' + validation + " failed: '" + message + "'.");
    e['validation'] = validation;
    e['validatedValue'] = value;

    return e;
};

export const Check = {
    exists(value, message = 'Value does not exist'): any {
        if (!Verify.exists(value)) {
            throw createCheckError('exists', value, message);
        }

        return value;
    },

    isString(value, message = 'Value is not a string'): string {
        if (!Verify.isString(value)) {
            throw createCheckError('isString', value, message);
        }

        return value;
    },

    notEmpty(value, message = 'The value is empty'): string {
        if (!Verify.minLength(value, 1)) {
            throw createCheckError('notEmpty', value, message);
        }

        return value;
    }
};
