import { DotCMSContentlet, DotFileMetadata, DotCMSTempFile } from '@dotcms/dotcms-models';
/**
 * Returns the metadata associated with the given contentlet.
 *
 * If the contentlet has a metaData property, that is returned. Otherwise,
 * the assetMetaData property is returned. If neither property is present,
 * an empty object is returned.
 *
 * @param contentlet A DotCMSContentlet object.
 * @returns A DotFileMetadata object.
 */
export const getFileMetadata = (contentlet: DotCMSContentlet): DotFileMetadata => {
    const { metaData } = contentlet;

    const metadata = metaData || contentlet[`assetMetaData`];

    return metadata || {};
};

/**
 * Returns the version of the file associated with the given contentlet.
 *
 * If the contentlet has a assetVersion property, that is returned. Otherwise,
 * null is returned.
 *
 * @param contentlet A DotCMSContentlet object.
 * @returns The version of the file associated with the contentlet, or null.
 */
export const getFileVersion = (contentlet: DotCMSContentlet) => {
    return contentlet['assetVersion'] || contentlet['fileAssetVersion'] || null;
};

/**
 * Given an array of mime types, this function will convert all of them to
 * lower case and remove any asterisks from the mime type. It will then
 * filter out any empty strings from the array.
 *
 * @param mimeTypes an array of mime types
 * @returns an array of cleaned mime types
 */
export function cleanMimeTypes(mimeTypes: string[]): string[] {
    return mimeTypes
        .map((type) => {
            return type.toLowerCase().replace(/\*/g, '');
        })
        .filter((type) => type !== '');
}

/**
 * Checks if a file's mime type is in a list of accepted mime types.
 *
 * If the acceptedFiles array is empty, then this function will return true.
 *
 * @param file the file to check, either a DotCMSTempFile or DotCMSContentlet
 * @param acceptedFiles an array of mime types to check against
 * @returns true if the file's mime type is in the list of accepted mime types, false otherwise
 */
export function checkMimeType(
    file: DotCMSTempFile | DotCMSContentlet,
    acceptedFiles: string[]
): boolean {
    if (acceptedFiles.length === 0) {
        return true;
    }

    const mimeTypes = cleanMimeTypes(acceptedFiles);
    const mimeType = file.mimeType;

    if (mimeType) {
        return mimeTypes.some((type) => mimeType.includes(type));
    }

    return false;
}

/**
 * Common image file extensions, used as a last-resort fallback when metadata
 * carries neither an explicit `isImage` flag nor a recognizable `image/*`
 * content type.
 */
const IMAGE_FILE_EXTENSIONS = new Set([
    'jpg',
    'jpeg',
    'png',
    'gif',
    'webp',
    'bmp',
    'svg',
    'ico',
    'tif',
    'tiff',
    'avif',
    'heic',
    'heif'
]);

/**
 * Whether a file name ends with a known image extension.
 *
 * @param name The file name to inspect.
 * @returns true when the name has a recognized image extension.
 */
function hasImageExtension(name: string | undefined): boolean {
    const extension = name?.split('.').pop()?.toLowerCase();

    return !!extension && IMAGE_FILE_EXTENSIONS.has(extension);
}

/**
 * Determines whether a file's metadata describes an image, using layered signals
 * so detection stays correct even when the metadata is partial:
 *
 * 1. the authoritative server-computed `isImage` flag;
 * 2. an `image/*` content type;
 * 3. a known image file extension on the file name (last-resort fallback).
 *
 * Use this to gate image-only affordances (e.g. the image editor) uniformly
 * across Binary fields and the `dotAsset` referenced by Image/File fields. Pair
 * it with {@link getFileMetadata} to resolve the metadata first.
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

    if (metadata.contentType?.toLowerCase().startsWith('image/')) {
        return true;
    }

    return hasImageExtension(metadata.name);
}
