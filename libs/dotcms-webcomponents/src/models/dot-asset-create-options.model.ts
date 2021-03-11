import { DotCMSTempFile } from 'dotcms-models';

export interface DotAssetCreateOptions {
    files: DotCMSTempFile[];
    updateCallback: (processed: number) => void;
    url: string;
    folder: string;
}
