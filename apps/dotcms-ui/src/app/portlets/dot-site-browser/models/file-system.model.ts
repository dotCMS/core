export interface FileSystemFileEntry extends FileSystemEntry {
    isDirectory: false;
    isFile: true;
    file(callback: (file: File) => void): void;
}

export interface FileSystemDirectoryEntry extends FileSystemEntry {
    isDirectory: true;
    isFile: false;
    createReader(): FileSystemDirectoryReader;
    getFile(
        path?: string,
        options?: FileSystemFlags,
        successCallback?: (result: FileSystemFileEntry) => void,
        errorCallback?: (error: DOMError) => void
    ): void;
    getDirectory(
        path?: string,
        options?: FileSystemFlags,
        successCallback?: (result: FileSystemDirectoryEntry) => void,
        errorCallback?: (error: DOMError) => void
    ): void;
}

export interface FileSystemFlags {
    create?: boolean;
    exclusive?: boolean;
}

export interface FileSystemDirectoryReader {
    readEntries(
        successCallback: (result: FileSystemEntry[]) => void,
        errorCallback?: (error: DOMError) => void
    ): void;
}

export interface FileSystemEntry {
    name: string;
    isDirectory: boolean;
    isFile: boolean;
}
