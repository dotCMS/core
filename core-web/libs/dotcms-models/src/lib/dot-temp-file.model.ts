/**
 * Interface that represent the response of /api/v1/temp, endpoint to upload temporary files.
 *
 * @interface
 */
export interface DotCMSTempFile {
    fileName: string;
    folder: string;
    id: string;
    image: boolean;
    length: number;
    mimeType: string;
    referenceUrl: string;
    thumbnailUrl: string;
}
