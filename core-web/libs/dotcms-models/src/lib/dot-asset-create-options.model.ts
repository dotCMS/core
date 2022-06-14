import { DotCMSTempFile } from './dot-temp-file.model';

export interface DotAssetCreateOptions {
    files: DotCMSTempFile[];
    updateCallback: (processed: number) => void;
    url: string;
    folder: string;
}
