import { DotCMSTempFile } from 'dotcms-models';
import { DotHttpErrorResponse } from '../../../models/dot-http-error-response.model';

export class DotUploadService {
    constructor() {}


    /**
     * Will call the corresponding endpoint yo upload a temporary file.
     * Return the information of tha file in the server
     * @param file
     *
     * @memberof DotUploadService
     */
    uploadFile(file: string | File, maxSize?: string): Promise<DotCMSTempFile> {
        if (typeof file === 'string') {
            return this.uploadFileByURL(file);
        } else {
            return this.uploadBinaryFile(file, maxSize);
        }
    }

    private uploadFileByURL(url: string): Promise<DotCMSTempFile> {
        const UPLOAD_FILE_FROM_URL = '/api/v1/temp/byUrl';
        return fetch(UPLOAD_FILE_FROM_URL, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                Origin: window.location.hostname
            },
            body: JSON.stringify({
                remoteUrl: url
            })
        }).then(async (response: Response) => {
            if (response.status === 200) {
                return (await response.json()).tempFiles[0];
            } else {
                const error: DotHttpErrorResponse = {
                    message: (await response.json()).message,
                    status: response.status
                };
                throw error;
            }
        });
    }

    private uploadBinaryFile(file: File, maxSize?: string): Promise<DotCMSTempFile> {
        let path = `/api/v1/temp`;
        path += maxSize ? `?maxFileLength=${maxSize}` : '';
        const formData = new FormData();
        formData.append('file', file);
        return fetch(path, {
            method: 'POST',
            headers: {
                Origin: window.location.hostname
            },
            body: formData
        }).then(async (response: Response) => {
            if (response.status === 200) {
                return (await response.json()).tempFiles[0];
            } else {
                const error: DotHttpErrorResponse = {
                    message: (await response.json()).message,
                    status: response.status
                };
                throw error;
            }
        });
    }
}
