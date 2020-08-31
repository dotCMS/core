export class DotUploadService {
    constructor() { }
    uploadFile(file, maxSize) {
        if (typeof file === 'string') {
            return this.uploadFileByURL(file);
        }
        else {
            return this.uploadBinaryFile(file, maxSize);
        }
    }
    uploadFileByURL(url) {
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
        }).then(async (response) => {
            if (response.status === 200) {
                return (await response.json()).tempFiles[0];
            }
            else {
                const error = {
                    message: (await response.json()).message,
                    status: response.status
                };
                throw error;
            }
        });
    }
    uploadBinaryFile(file, maxSize) {
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
        }).then(async (response) => {
            if (response.status === 200) {
                return (await response.json()).tempFiles[0];
            }
            else {
                const error = {
                    message: (await response.json()).message,
                    status: response.status
                };
                throw error;
            }
        });
    }
}
