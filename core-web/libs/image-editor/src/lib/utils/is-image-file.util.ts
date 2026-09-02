import { DotFileMetadata } from '@dotcms/dotcms-models';

/**
 * Determines whether a file's metadata describes an image.
 *
 * Trusts the authoritative server-computed `isImage` flag first, then falls back
 * to the `image/*` content type. Both signals are reliably present on the
 * metadata this gate runs against: the `assetMetaData` of the `dotAsset`
 * referenced by Image/File fields, and the (enriched) metadata of Binary temp
 * files. Extension sniffing is intentionally avoided — file names are unreliable
 * and the mime type is always available here.
 *
 * @param metadata The resolved file metadata (may be partial, null or undefined).
 * @returns true when the metadata describes an image.
 */
export function isImageFile(metadata: Partial<DotFileMetadata> | null | undefined): boolean {
    if (!metadata) {
        return false;
    }

    if (metadata.isImage) {
        return true;
    }

    return !!metadata.contentType?.toLowerCase().startsWith('image/');
}
