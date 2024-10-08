import { DotCMSContentlet, DotCMSTempFile } from '@dotcms/dotcms-models';

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
