import { AbstractControl, ValidationErrors } from '@angular/forms';

/** True when the input parses as a syntactically valid http(s) URL. */
export function isValidHttpUrl(value: string): boolean {
    try {
        const parsed = new URL(value);
        return parsed.protocol === 'http:' || parsed.protocol === 'https:';
    } catch {
        return false;
    }
}

/**
 * True when the input is a valid hyperlink target for editorial content.
 * Accepts http/https/mailto/tel schemes, anchor links (`#section`), and
 * relative paths (`/page`, `./page`, `../page`). Used by the link popover
 * so customers can link to mailto/tel/anchor/relative destinations — the
 * legacy editor allowed those, the new editor must too.
 */
export function isValidLinkHref(value: string): boolean {
    const trimmed = value.trim();
    if (!trimmed) return false;
    if (trimmed.startsWith('#') || trimmed.startsWith('/')) return true;
    if (trimmed.startsWith('./') || trimmed.startsWith('../')) return true;
    try {
        const u = new URL(trimmed);

        return ['http:', 'https:', 'mailto:', 'tel:'].includes(u.protocol);
    } catch {
        return false;
    }
}

/** Reactive Forms validator for {@link isValidLinkHref}. Empty value passes — pair with `Validators.required`. */
export function linkHrefValidator(control: AbstractControl): ValidationErrors | null {
    const value = (control.value ?? '').toString().trim();
    if (!value) return null;

    return isValidLinkHref(value) ? null : { invalidUrl: true };
}
