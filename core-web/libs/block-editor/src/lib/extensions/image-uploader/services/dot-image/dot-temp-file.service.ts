import { Injectable } from '@angular/core';

import { DotCMSTempFile, DotHttpErrorResponse, DotHttpRequestOptions } from '@dotcms/dotcms-models';

export const fallbackErrorMessages = {
    500: '500 Internal Server Error',
    400: '400 Bad Request',
    401: '401 Unauthorized Error'
};

export interface UploadTempFileProps<T = string | File | File[]> {
    file: T;
    progressCallBack?;
    maxSize?: string;
}

const TEMP_API_URL = '/api/v1/temp';

@Injectable({
    providedIn: 'root'
})
export class DotUploadFileService {
    private _currentHXHR: XMLHttpRequest;

    get currentHXHR(): XMLHttpRequest {
        return this._currentHXHR;
    }

    /**
     * Will call the corresponding endpoint to upload a temporary file.
     * Return the information of tha file in the server
     * @param {UploadTempFileProps} { file, progressCallBack, maxSize }
     * @return {*}  {Promise<DotCMSTempFile>}
     * @memberof DotUploadFileService
     */
    uploadFile({ file, progressCallBack, maxSize }: UploadTempFileProps): Promise<DotCMSTempFile> {
        return typeof file === 'string'
            ? this.uploadFileByURL(file)
            : (this.uploadBinaryFile({
                  file,
                  progressCallBack,
                  maxSize
              }) as Promise<DotCMSTempFile>);
    }

    /**
     *
     *
     * @private
     * @param {string} url
     * @return {*}  {Promise<DotCMSTempFile>}
     * @memberof DotUploadFileService
     */
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
     *  Will call the temp resource endpoint to upload a temporary file.
     * With a callback to track the progress of the upload
     * Return the information of tha file(s) in the server
     *
     * @private
     * @param {(UploadTempFileProps<File | File[]>)} {
     *         file: data,
     *         progressCallBack,
     *         maxSize
     *     }
     * @return {*}  {(Promise<DotCMSTempFile | DotCMSTempFile[]>)}
     * @memberof DotUploadFileService
     */
    private uploadBinaryFile({
        file: data,
        progressCallBack,
        maxSize
    }: UploadTempFileProps<File | File[]>): Promise<DotCMSTempFile | DotCMSTempFile[]> {
        let path = TEMP_API_URL;
        path += maxSize ? `?maxFileLength=${maxSize}` : '';
        const formData = new FormData();

        const files = Array.isArray(data) ? data : [data];

        files.forEach((file: File) => formData.append('files', file));

        return this.dotRequest(
            path,
            {
                method: 'POST',
                headers: {},
                body: formData
            },
            progressCallBack
        )
            .then((request: XMLHttpRequest) => {
                if (request.status === 200) {
                    const data = JSON.parse(request.response).tempFiles;

                    return data.length > 1 ? data : data[0];
                } else {
                    throw request;
                }
            })
            .catch((request: XMLHttpRequest) => {
                throw this.errorHandler(JSON.parse(request.response), request.status);
            });
    }

    /**
     *
     *
     * @private
     * @param {string} url
     * @param {DotHttpRequestOptions} opts
     * @param {(progress: number) => {}} progressCallBack
     * @return {*}  {Promise<XMLHttpRequest>}
     * @memberof DotUploadFileService
     */
    private dotRequest(
        url: string,
        opts: DotHttpRequestOptions,
        progressCallBack: (progress: number) => {
            /* */
        }
    ): Promise<XMLHttpRequest> {
        return new Promise((res, rej) => {
            this._currentHXHR = new XMLHttpRequest();
            this._currentHXHR.open(opts.method || 'get', url);
            for (const name in opts.headers || {}) {
                this._currentHXHR.setRequestHeader(name, opts.headers[name]);
            }

            this._currentHXHR.onload = () => res(this._currentHXHR);
            this._currentHXHR.onerror = rej;
            if (this._currentHXHR.upload && progressCallBack) {
                this._currentHXHR.upload.onprogress = (e: ProgressEvent) => {
                    const percentComplete = (e.loaded / e.total) * 100;
                    progressCallBack(percentComplete);
                };
            }

            this._currentHXHR.send(opts.body);
        });
    }

    /**
     *
     *
     * @private
     * @param {Record<string, string>} response
     * @param {number} status
     * @return {*}  {DotHttpErrorResponse}
     * @memberof DotUploadFileService
     */
    private errorHandler(response: Record<string, string>, status: number): DotHttpErrorResponse {
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
