import { FileSystemEntry } from './file-system.model';

export class DotUploadFile {
    constructor(public relativePath: string, public fileEntry: FileSystemEntry) {}
}
