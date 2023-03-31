import { DotCMSTempFile } from './dot-temp-file.model';

export enum DotFileCurrentStatus {
    UPLOADFILE = 'UploadFile',
    CODEEDITOR = 'CodeEditor',
    FILELIST = 'FileList'
}

export interface DotUploadFile {
    currentState: DotFileCurrentStatus;
    assets: DotCMSTempFile[];
}
