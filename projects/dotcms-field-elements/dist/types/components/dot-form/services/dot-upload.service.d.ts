import { DotCMSTempFile } from 'dotcms-models';
export declare class DotUploadService {
    constructor();
    /**
     * Will call the corresponding endpoint yo upload a temporary file.
     * Return the information of tha file in the server
     * @param file
     *
     * @memberof DotUploadService
     */
    uploadFile(file: string | File, maxSize?: string): Promise<DotCMSTempFile>;
    private uploadFileByURL;
    private uploadBinaryFile;
}
