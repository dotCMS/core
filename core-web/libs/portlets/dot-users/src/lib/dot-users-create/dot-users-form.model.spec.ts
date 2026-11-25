import { FormControl } from '@angular/forms';

import {
    generateSecurePassword,
    PASSWORD_MAX_LENGTH,
    PASSWORD_MIN_LENGTH,
    passwordComplexityValidator
} from './dot-users-form.model';

/** Same rule as the backend default, written as a JS regular expression. */
const BACKEND_DEFAULT = /^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9 ])[ -~]{15,64}$/;

describe('dot-users-form.model', () => {
    describe('password length constants', () => {
        it('should match the backend default of 15 to 64 characters', () => {
            expect(PASSWORD_MIN_LENGTH).toBe(15);
            expect(PASSWORD_MAX_LENGTH).toBe(64);
        });
    });

    describe('generateSecurePassword', () => {
        it('should return 16 characters with one of each class, no space, over many runs', () => {
            for (let i = 0; i < 1000; i++) {
                const password = generateSecurePassword();

                expect(password).toHaveLength(16);
                expect(password).toMatch(/[A-Z]/);
                expect(password).toMatch(/[a-z]/);
                expect(password).toMatch(/[0-9]/);
                expect(password).toMatch(/[^A-Za-z0-9 ]/);
                expect(password).not.toContain(' ');
                expect(BACKEND_DEFAULT.test(password)).toBe(true);
            }
        });
    });

    describe('passwordComplexityValidator', () => {
        const validate = (value: string) => passwordComplexityValidator(new FormControl(value));

        it('should accept an empty value, which required and edit mode handle', () => {
            expect(validate('')).toBeNull();
        });

        it('should accept a password with an uppercase, a lowercase, a digit and a special character', () => {
            expect(validate('Aa1!bcdefghijkl')).toBeNull();
            expect(validate('Aa1_bcdefghijkl')).toBeNull();
        });

        it('should report passwordComplexity when one class is missing', () => {
            expect(validate('aa1!bcdefghijkl')).toEqual({ passwordComplexity: true });
            expect(validate('AA1!BCDEFGHIJKL')).toEqual({ passwordComplexity: true });
            expect(validate('Aa!!bcdefghijkl')).toEqual({ passwordComplexity: true });
            expect(validate('Aa11bcdefghijkl')).toEqual({ passwordComplexity: true });
        });

        it('should not count a space as a special character', () => {
            expect(validate('Aa1 bcdefghijkl')).toEqual({ passwordComplexity: true });
        });
    });
});
