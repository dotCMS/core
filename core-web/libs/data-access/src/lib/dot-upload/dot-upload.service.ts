import { Injectable } from '@angular/core';

import { DotCMSTempFile, DotHttpErrorResponse } from '@dotcms/dotcms-models';

export const fallbackErrorMessages: { [key: number]: string } = {
    500: '500 Internal Server Error',
    400: '400 Bad Request',
    401: '401 Unauthorized Error'
};
export interface UploadFileProps {
    file: string | File;
    maxSize?: string;
    signal?: AbortSignal;
}
export interface ErrorResponse {
    message: string;
    errors: { message: string }[];
}
@Injectable({ providedIn: 'root' })
export class DotUploadService {
    /**
     * Upload a file to the server
     *
     * @param {UploadFileProps} {file, maxSize, signal}
     * @return {*}  {Promise<DotCMSTempFile>}
     * @memberof DotUploadService
     */
    uploadFile({ file, maxSize, signal }: UploadFileProps): Promise<DotCMSTempFile> {
        if (typeof file === 'string') {
            return this.uploadFileByURL(file, signal);
        } else {
            return this.uploadBinaryFile({ file, maxSize, signal });
        }
    }

    /**
     * Upload a file to the server by URL
     *
     * @private
     * @param {string} url
     * @param {signal} [AbortSignal]
     * @return {*}  {Promise<DotCMSTempFile>}
     * @memberof DotUploadService
     */
    private uploadFileByURL(url: string, signal?: AbortSignal): Promise<DotCMSTempFile> {
        const UPLOAD_FILE_FROM_URL = '/api/v1/temp/byUrl';

        return fetch(UPLOAD_FILE_FROM_URL, {
            method: 'POST',
            signal,
            headers: {
                'Content-Type': 'application/json',
                Origin: window.location.hostname
            },
            body: JSON.stringify({
                remoteUrl: url
            })
        })
            .then(async (response: Response) => {
                if (response.status === 200) {
                    return (await response.json()).tempFiles[0];
                } else {
                    const error: DotHttpErrorResponse = {
                        message: (await response.json()).message,
                        status: response.status
                    };
                    throw error;
                }
            })
            .catch((request) => {
                const { message, response } = request;
                const parsedResponse =
                    typeof response === 'string' ? JSON.parse(response) : response;

                throw this.errorHandler(parsedResponse || { message }, request.status);
            });
    }

    /**
     * Upload a binary file to the server
     *
     * @private
     * @param {UploadFileProps} {file, maxSize, abortControler}
     * @return {*}  {Promise<DotCMSTempFile>}
     * @memberof DotUploadService
     */
    private uploadBinaryFile({ file, maxSize, signal }: UploadFileProps): Promise<DotCMSTempFile> {
        let path = `/api/v1/temp`;
        path += maxSize ? `?maxFileLength=${maxSize}` : '';
        const formData = new FormData();
        formData.append('file', file);

        return fetch(path, {
            method: 'POST',
            signal,
            headers: {
                Origin: window.location.hostname
            },
            body: formData
        })
            .then(async (response: Response) => {
                if (response.status === 200) {
                    return (await response.json()).tempFiles[0];
                } else {
                    const error: DotHttpErrorResponse = {
                        message: (await response.json()).message,
                        status: response.status
                    };
                    throw error;
                }
            })
            .catch((request) => {
                throw this.errorHandler(JSON.parse(request.response), request.status);
            });
    }

    /**
     * Handle error response
     *
     * @private
     * @param {*} response
     * @param {number} status
     * @return {*}  {DotHttpErrorResponse}
     * @memberof DotUploadService
     */
    private errorHandler(response: ErrorResponse, status: number): DotHttpErrorResponse {
        let message = '';
        try {
            message = response.message || response.errors[0].message;
        } catch {
            message = fallbackErrorMessages[status || 500];
        }

        return {
            message: message,
            status: status | 500
        };
    }
}
