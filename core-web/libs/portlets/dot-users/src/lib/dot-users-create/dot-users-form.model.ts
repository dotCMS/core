import { AbstractControl, FormControl, FormGroup, ValidationErrors } from '@angular/forms';

/**
 * Value-shape interfaces used to type the reactive form. `FormGroup`
 * accepts a control shape (see {@link DotUsersFormGroup}), so these
 * interfaces stay concise and drive both — the FormGroup type below
 * derives its controls from them via {@link ControlsOf}.
 */
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
    showGettingStarted: boolean;
}

export interface DotUsersForm {
    account: DotUsersAccountForm;
    additionalInfo: DotUsersAdditionalInfoForm;
    access: DotUsersAccessForm;
}

/**
 * Maps a flat value interface (leaves are primitives) to the matching
 * shape of `FormControl`s used inside a `FormGroup<T>`.
 */
type ControlsOf<T extends Record<string, unknown>> = {
    [K in keyof T]: FormControl<T[K]>;
};

export type DotUsersAccountGroup = FormGroup<ControlsOf<DotUsersAccountForm>>;
export type DotUsersAdditionalInfoGroup = FormGroup<ControlsOf<DotUsersAdditionalInfoForm>>;
export type DotUsersAccessGroup = FormGroup<ControlsOf<DotUsersAccessForm>>;

/**
 * Fully-typed reactive form used by the Create/Edit User dialog. The
 * shell owns it and passes the same reference to the Profile tab as an
 * `input.required<DotUsersFormGroup>()`, which restores end-to-end
 * type-safety on `formGroupName` / `formControlName` bindings.
 */
export type DotUsersFormGroup = FormGroup<{
    account: DotUsersAccountGroup;
    additionalInfo: DotUsersAdditionalInfoGroup;
    access: DotUsersAccessGroup;
}>;

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
 * Generates a 12-character password with a mixed character set. Used
 * by the "Generate secure password" action; kept in a shared module so
 * the profile tab and the create component agree on the same seed.
 *
 * Uses `crypto.getRandomValues` with rejection sampling so each pick is
 * uniform across the source alphabet — `% source.length` would bias the
 * distribution when 256 doesn't divide evenly. The final shuffle is a
 * Fisher–Yates pass over the same CSPRNG, so the four seed classes
 * aren't stuck at fixed positions.
 */
export function generateSecurePassword(): string {
    const upper = 'ABCDEFGHJKLMNPQRSTUVWXYZ';
    const lower = 'abcdefghijkmnopqrstuvwxyz';
    const digits = '23456789';
    const symbols = '!@#$%^&*';
    const all = upper + lower + digits + symbols;

    const pick = (source: string) => source.charAt(randomIndex(source.length));

    const seed = [pick(upper), pick(lower), pick(digits), pick(symbols)];
    for (let i = 0; i < 8; i++) {
        seed.push(pick(all));
    }

    for (let i = seed.length - 1; i > 0; i--) {
        const j = randomIndex(i + 1);
        [seed[i], seed[j]] = [seed[j], seed[i]];
    }

    return seed.join('');
}

/**
 * Uniform random integer in [0, max) using a CSPRNG. Rejects values in
 * the unusable tail of the 8-bit range so `% max` doesn't skew towards
 * the lower buckets when 256 is not divisible by `max`.
 */
function randomIndex(max: number): number {
    const limit = 256 - (256 % max);
    const buf = new Uint8Array(1);
    while (true) {
        crypto.getRandomValues(buf);
        if (buf[0] < limit) {
            return buf[0] % max;
        }
    }
}
