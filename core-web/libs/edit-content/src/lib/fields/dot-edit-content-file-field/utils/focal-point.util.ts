import { DotCMSContentlet, DotFileMetadata } from '@dotcms/dotcms-models';
import { NormalizedPoint } from '@dotcms/image-editor';

/**
 * Reads the focal point string from a resolved binary metadata object.
 *
 * The backend surfaces it under the single clean `focalPoint` key on the binary
 * field's metadata — the same mechanism Binary fields have always used.
 *
 * @param metadata - The resolved file metadata.
 * @returns The focal point `"x,y"` string, or undefined when none is present.
 */
export function focalPointFromMetadata(
    metadata: DotFileMetadata | null | undefined
): string | undefined {
    return metadata?.focalPoint;
}

/**
 * Resolves the focal point from a contentlet that carries binary metadata.
 *
 * The focal lives on the binary field's metadata, exposed by the backend as
 * `{fieldVar}MetaData`. Which field that is depends on what the contentlet is:
 *
 * - a referenced asset (dotAsset or legacy FileAsset): its own binary field (`asset` /
 *   `fileAsset`), carried by the contentlet's `titleImage`;
 * - the PARENT contentlet of an inline Binary field: the clicked field itself, passed as
 *   `fieldVariable`. The parent's `titleImage` names its title image, which may be
 *   another binary field of the same item, so it must not be used here.
 *
 * @param file - The contentlet holding the `{fieldVar}MetaData` entries.
 * @param fieldVariable - The binary field to read; when omitted the contentlet's
 * `titleImage` (default `asset`) is used.
 * @returns The focal point `"x,y"` string, or undefined when none is present.
 */
export function focalPointFromContentlet(
    file: DotCMSContentlet | null | undefined,
    fieldVariable?: string
): string | undefined {
    if (!file) {
        return undefined;
    }

    const raw = file as unknown as Record<string, DotFileMetadata | undefined>;
    const fieldVar =
        fieldVariable ||
        ((file as unknown as Record<string, string>)['titleImage'] ?? 'asset') ||
        'asset';

    return focalPointFromMetadata(raw[`${fieldVar}MetaData`]);
}

/**
 * Parses a focal point stored as an `"x,y"` string (the backend exposes it on the
 * binary field metadata as a custom `focalPoint` attribute) into a normalized 0..1
 * point for seeding the image editor.
 *
 * Returns `undefined` for an unset/empty value, the `(0,0)` "no focal point" sentinel,
 * or a malformed/single-value string, so the editor opens centred instead of seeding a
 * bogus marker.
 *
 * @param value - The raw `focalPoint` metadata string (e.g. `"0.88,0.31"`).
 * @returns The parsed point, or `undefined` when there is no usable focal point.
 */
export function parseFocalPoint(value: string | null | undefined): NormalizedPoint | undefined {
    if (!value) {
        return undefined;
    }

    const [x, y] = value.split(',').map(Number);

    if (!Number.isFinite(x) || !Number.isFinite(y)) {
        return undefined;
    }

    // (0,0) is the backend's "no focal point" sentinel -> open centred.
    if (x === 0 && y === 0) {
        return undefined;
    }

    return { x, y };
}
