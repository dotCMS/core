/** Crockford base32 alphabet used by ULIDs (no I, L, O, U). */
const CROCKFORD = '0123456789ABCDEFGHJKMNPQRSTVWXYZ';

const ULID_PATTERN = /^[0-9A-HJKMNP-TV-Z]{26}$/i;

/** Earliest timestamp accepted as real. A 26-character id that only happens to
 * use the ULID alphabet would otherwise decode to an arbitrary date. */
const MIN_PLAUSIBLE_MS = Date.UTC(2000, 0, 1);

/** Allowed clock skew between the server that minted the id and this browser. */
const MAX_FUTURE_SKEW_MS = 24 * 60 * 60 * 1000;

/**
 * Returns the creation time encoded in a ULID, as an ISO 8601 string.
 *
 * Bundle ids are minted as ULIDs (`BundleFactoryImpl` → `new ULID().nextULID()`),
 * whose first 10 characters are the millisecond timestamp. `publishing_bundle`
 * has no creation date column, so this is the only place an unsent bundle's
 * creation time can come from.
 *
 * @param id a bundle id
 * @returns the ISO timestamp, or `null` when `id` is not a ULID (e.g. an uploaded
 *          bundle that kept its own id) or decodes to an implausible date
 */
export function ulidTimestamp(id: string | null | undefined): string | null {
    if (!id || !ULID_PATTERN.test(id)) {
        return null;
    }

    let ms = 0;
    for (const char of id.slice(0, 10).toUpperCase()) {
        ms = ms * 32 + CROCKFORD.indexOf(char);
    }

    if (ms < MIN_PLAUSIBLE_MS || ms > Date.now() + MAX_FUTURE_SKEW_MS) {
        return null;
    }

    return new Date(ms).toISOString();
}
