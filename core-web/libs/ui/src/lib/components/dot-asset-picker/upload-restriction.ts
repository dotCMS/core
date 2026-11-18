import { ASSET_PICKER_MIME_TYPES } from './asset-picker-config';

/**
 * Applies the picker's existing mimetype narrowing to uploads.
 *
 * The restriction is not a new concept: `config.mimeTypes` already decides what the editor may
 * *browse*, and this is the same value deciding what they may *add*. Nothing here knows about an
 * "Image field" or a "video node" — presence of a restriction is the whole input, which is what
 * keeps the File field and the browse entry point unrestricted without a single `mode === 'x'`.
 *
 * Pure on purpose, like {@link ./asset-picker-config} and {@link ./last-asset-path}: the message
 * lookup arrives as a parameter rather than an injected `DotMessageService`, so this is testable
 * without a component harness. Not exported from the library barrel — it has no consumer outside
 * the picker.
 */

const MIME_LABEL_KEY_PREFIX = 'dot.asset.picker.upload.types.';

/**
 * The type families that have a human label.
 *
 * Derived from {@link ASSET_PICKER_MIME_TYPES} rather than written out again, so adding a media
 * mode there is the only edit needed — the family becomes labellable and only its message key has
 * to follow. A `browse` caller may pass anything else; those fall back to their raw pattern.
 */
const LABELLED_FAMILIES: ReadonlySet<string> = new Set(Object.keys(ASSET_PICKER_MIME_TYPES));

/** `image/*` and `image/png` are both the `image` family. */
const familyOf = (pattern: string): string => pattern.trim().toLowerCase().split('/')[0];

/**
 * Whether a file may be uploaded under the given restriction.
 *
 * Two things are deliberately permissive, and both are load-bearing:
 *
 * - **No restriction accepts everything.** Absence, not a sentinel, is the unrestricted state —
 *   the File field and `browse` arrive here with nothing, and a guard that defaulted to refusing
 *   would break them silently.
 * - **A file the browser reports no type for is accepted.** The server remains the authority;
 *   refusing would occasionally block a legitimate image behind a message saying only images are
 *   allowed, with no way forward. (`DotDropZoneComponent.typeMatch` rejects in that case — the
 *   divergence is intentional.)
 *
 * The filename is never consulted. Matching on extensions would mean maintaining a second list of
 * types alongside the one the browse filter already uses, which is the thing this whole module
 * exists to avoid.
 */
export function isUploadAllowed(file: File, mimeTypes?: string[]): boolean {
    if (!mimeTypes?.length) {
        return true;
    }

    const type = file.type?.trim().toLowerCase();

    if (!type) {
        return true;
    }

    return mimeTypes.some((pattern) => {
        const candidate = pattern.trim().toLowerCase();

        // `image/*` matches the family, and only as a prefix — a substring test would also let
        // `x-image/foo` through.
        return candidate.endsWith('/*')
            ? type.startsWith(candidate.slice(0, -1))
            : type === candidate;
    });
}

/**
 * The `accept` value for the hidden file input, or `null` when nothing is restricted.
 *
 * The patterns pass through verbatim: `accept` takes the same `type/*` syntax the browse filter
 * already uses, so there is nothing to translate.
 *
 * `null` rather than `''` so the binding *removes* the attribute — an empty `accept` is not the
 * same thing to the browser as no `accept`.
 */
export function buildUploadAccept(mimeTypes?: string[]): string | null {
    return mimeTypes?.length ? mimeTypes.join(',') : null;
}

/**
 * Names the restriction in words an author can read — "images", not `image/*`.
 *
 * Falls back to the raw pattern for a family with no label, so a `browse` caller passing something
 * exotic still produces a correct message instead of "Only  can be uploaded here."
 */
export function resolveUploadRestrictionLabel(
    mimeTypes: string[] | undefined,
    translate: (key: string) => string
): string | undefined {
    if (!mimeTypes?.length) {
        return undefined;
    }

    const labels: string[] = [];

    for (const pattern of mimeTypes) {
        const family = familyOf(pattern);
        const label = LABELLED_FAMILIES.has(family)
            ? translate(`${MIME_LABEL_KEY_PREFIX}${family}`)
            : pattern.trim();

        // Two patterns in the same family resolve to one label; say it once.
        if (!labels.includes(label)) {
            labels.push(label);
        }
    }

    return labels.join(', ');
}
