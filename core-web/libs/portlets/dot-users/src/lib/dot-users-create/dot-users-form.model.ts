import { AbstractControl, FormGroup, ValidationErrors } from '@angular/forms';

export interface DotUsersAccountForm {
    firstName: string;
    lastName: string;
    email: string;
    password: string;
    confirmPassword: string;
    active: boolean;
}

export interface DotUsersAdditionalInfoForm {
    prefix: string;
    suffix: string;
    title: string;
    company: string;
    website: string;
}

export interface DotUsersAccessForm {
    cmsAdmin: boolean;
    backend: boolean;
    frontend: boolean;
    canLogin: boolean;
    showGettingStarted: boolean;
}

export interface DotUsersForm {
    account: DotUsersAccountForm;
    additionalInfo: DotUsersAdditionalInfoForm;
    access: DotUsersAccessForm;
}

/**
 * Cross-field validator applied to the `account` sub-group. Emits the
 * `passwordMismatch` error when a password was entered and the confirm
 * value does not match. An empty password is legal — in edit mode it
 * signals "keep current password" and skips validation.
 */
export function passwordsMatchValidator(control: AbstractControl): ValidationErrors | null {
    const group = control as FormGroup;
    const password = group.get('password')?.value ?? '';
    const confirm = group.get('confirmPassword')?.value ?? '';

    if (!password && !confirm) {
        return null;
    }

    return password === confirm ? null : { passwordMismatch: true };
}

/**
 * Generates an 12-character password with a mixed character set. Used
 * by the "Generate secure password" action; kept in a shared module so
 * the profile tab and the create component agree on the same seed.
 */
export function generateSecurePassword(): string {
    const upper = 'ABCDEFGHJKLMNPQRSTUVWXYZ';
    const lower = 'abcdefghijkmnopqrstuvwxyz';
    const digits = '23456789';
    const symbols = '!@#$%^&*';
    const all = upper + lower + digits + symbols;

    const pick = (source: string) => source.charAt(Math.floor(Math.random() * source.length));

    const seed = [pick(upper), pick(lower), pick(digits), pick(symbols)];
    for (let i = 0; i < 8; i++) {
        seed.push(pick(all));
    }

    return seed.sort(() => Math.random() - 0.5).join('');
}
