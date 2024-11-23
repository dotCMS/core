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
