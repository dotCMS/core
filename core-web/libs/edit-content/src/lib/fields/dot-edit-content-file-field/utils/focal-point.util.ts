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
 * Resolves the focal point from a referenced asset contentlet.
 *
 * The focal lives on the binary field's metadata, exposed by the backend as
 * `{fieldVar}MetaData`, where `fieldVar` is the asset's binary field (`asset` for a
 * dotAsset, `fileAsset` for a legacy FileAsset) — carried by the contentlet's
 * `titleImage`. This is the same `{fieldVar}MetaData.focalPoint` mechanism a Binary
 * field uses, applied uniformly.
 *
 * @param file - The referenced asset contentlet (dotAsset or FileAsset).
 * @returns The focal point `"x,y"` string, or undefined when none is present.
 */
export function focalPointFromContentlet(
    file: DotCMSContentlet | null | undefined
): string | undefined {
    if (!file) {
        return undefined;
    }

    const raw = file as unknown as Record<string, DotFileMetadata | undefined>;
    const fieldVar =
        ((file as unknown as Record<string, string>)['titleImage'] ?? 'asset') || 'asset';

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
