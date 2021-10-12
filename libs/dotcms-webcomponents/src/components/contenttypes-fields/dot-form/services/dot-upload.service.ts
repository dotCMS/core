import { DotCMSTempFile } from '@dotcms/dotcms-models';
import { DotHttpErrorResponse } from '../../../../models/dot-http-error-response.model';
import { DotHttpRequestOptions } from '../../../../models/dot-http-request-options.model';

export const fallbackErrorMessages = {
    500: '500 Internal Server Error',
    400: '400 Bad Request',
    401: '401 Unauthorized Error'
};

const TEMP_API_URL = '/api/v1/temp';

export class DotUploadService {
    constructor() {}

    /**
     * Will call the corresponding endpoint to upload a temporary file.
     * Return the information of tha file in the server
     * @param file
     * @param maxSize
     *
     * @memberof DotUploadService
     */
    uploadFile(file: string | File, maxSize?: string): Promise<DotCMSTempFile> {
        if (typeof file === 'string') {
            return this.uploadFileByURL(file);
        } else {
            return this.uploadBinaryFile(file, maxSize) as Promise<DotCMSTempFile>;
        }
    }

    private uploadFileByURL(url: string): Promise<DotCMSTempFile> {
        const UPLOAD_FILE_FROM_URL = `${TEMP_API_URL}/byUrl`;
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
                throw this.errorHandler(await response.json(), response.status);
            }
        });
    }

    /**
     * Will call the temp resource endpoint to upload a temporary file.
     * With a callback to track the progress of the upload
     * Return the information of tha file(s) in the server
     * @param data
     * @param progressCallBack
     * @param maxSize
     *
     * @memberof DotUploadService
     */
    uploadBinaryFile(
        data: File | File[],
        progressCallBack?,
        maxSize?: string
    ): Promise<DotCMSTempFile | DotCMSTempFile[]> {
        let path = TEMP_API_URL;
        path += maxSize ? `?maxFileLength=${maxSize}` : '';
        const formData = new FormData();

        const files = Array.isArray(data) ? data : [data];
        files.forEach((file: File) => {
            formData.append('files', file);
        });

        return this.dotRequest(
            path,
            {
                method: 'POST',
                headers: {},
                body: formData
            },
            progressCallBack
        )
            .then(async (request: XMLHttpRequest) => {
                if (request.status === 200) {
                    const data = JSON.parse(request.response).tempFiles;
                    return data.length > 1 ? data : data[0];
                } else {
                    throw request;
                }
            })
            .catch((request) => {
                throw this.errorHandler(JSON.parse(request.response), request.status);
            });
    }

    private dotRequest(
        url: string,
        opts: DotHttpRequestOptions,
        progressCallBack: (progress: number) => {}
    ): Promise<XMLHttpRequest> {
        return new Promise((res, rej) => {
            const xhr = new XMLHttpRequest();
            xhr.open(opts.method || 'get', url);
            for (let name in opts.headers || {}) {
                xhr.setRequestHeader(name, opts.headers[name]);
            }
            xhr.onload = () => res(xhr);
            xhr.onerror = rej;
            if (xhr.upload && progressCallBack) {
                xhr.upload.onprogress = (e: ProgressEvent) => {
                    const percentComplete = (e.loaded / e.total) * 100;
                    progressCallBack(percentComplete);
                };
            }
            xhr.send(opts.body);
        });
    }

    private errorHandler(response: any, status: number): DotHttpErrorResponse {
        let message = '';
        try {
            message = response.message || fallbackErrorMessages[status];
        } catch (e) {
            message = fallbackErrorMessages[status || 500];
        }
        return {
            message: message,
            status: status | 500
        };
    }
}
