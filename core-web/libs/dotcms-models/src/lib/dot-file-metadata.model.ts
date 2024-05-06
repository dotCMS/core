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
}
