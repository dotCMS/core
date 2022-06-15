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

/**
 * Will call the corresponding endpoint to upload a temporary file.
 * Return the information of tha file in the server
 * @param file
 * @param maxSize
 *
 */
export function uploadFile({
    file,
    progressCallBack,
    maxSize
}: UploadTempFileProps): Promise<DotCMSTempFile> {
    if (typeof file === 'string') {
        return uploadFileByURL(file);
    } else {
        return uploadBinaryFile({ file, progressCallBack, maxSize }) as Promise<DotCMSTempFile>;
    }
}

function uploadFileByURL(url: string): Promise<DotCMSTempFile> {
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
            throw errorHandler(await response.json(), response.status);
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
 */
export function uploadBinaryFile({
    file: data,
    progressCallBack,
    maxSize
}: UploadTempFileProps<File | File[]>): Promise<DotCMSTempFile | DotCMSTempFile[]> {
    let path = TEMP_API_URL;
    path += maxSize ? `?maxFileLength=${maxSize}` : '';
    const formData = new FormData();

    const files = Array.isArray(data) ? data : [data];

    files.forEach((file: File) => formData.append('files', file));

    return dotRequest(
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
            throw errorHandler(JSON.parse(request.response), request.status);
        });
}

function dotRequest(
    url: string,
    opts: DotHttpRequestOptions,
    progressCallBack: (progress: number) => {
        /* */
    }
): Promise<XMLHttpRequest> {
    return new Promise((res, rej) => {
        const xhr = new XMLHttpRequest();
        xhr.open(opts.method || 'get', url);
        for (const name in opts.headers || {}) {
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

function errorHandler(response: Record<string, string>, status: number): DotHttpErrorResponse {
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
