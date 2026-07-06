import { DotFileMetadata } from '@dotcms/dotcms-models';
import { NormalizedPoint } from '@dotcms/image-editor';

/**
 * Resolves the focal point string from file metadata across its two shapes.
 *
 * A Binary field's own metadata surfaces the clean `focalPoint` key (added by the
 * backend view transform), whereas a referenced `dotAsset`'s `assetMetaData`
 * exposes the raw namespaced custom attribute `dot:focalPoint`. Reading both lets
 * the editor restore the marker on reopen regardless of which shape backs the field.
 *
 * @param metadata - The resolved file metadata (binary metaData or dotAsset assetMetaData).
 * @returns The focal point `"x,y"` string, or undefined when none is present.
 */
export function focalPointFromMetadata(
    metadata: DotFileMetadata | null | undefined
): string | undefined {
    if (!metadata) {
        return undefined;
    }

    return metadata.focalPoint ?? (metadata as unknown as Record<string, string>)['dot:focalPoint'];
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
