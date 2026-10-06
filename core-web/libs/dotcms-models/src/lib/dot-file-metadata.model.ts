export interface DotFileMetadata {
    contentType: string;
    fileSize: number;
    isImage: boolean;
    length: number;
    modDate: number;
    name: string;
    sha256: string;
    title: string;
    version: number;
    height?: number;
    width?: number;
    editableAsText?: boolean;
    /**
     * Focal point as an `"x,y"` string (normalized 0..1), exposed by the backend on image
     * binary metadata and used to re-seed the image editor's focal marker on reopen.
     */
    focalPoint?: string;
}
