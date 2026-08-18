import {
    DotAssetCreateOptions,
    DotCMSContentlet,
    DotCMSTempFile,
    DotHttpErrorResponse
} from '@dotcms/dotcms-models';
import { fallbackErrorMessages } from '../../components/contenttypes-fields/dot-form/services/dot-upload.service';

export class DotAssetService {
    /**
     * Create DotAssets based on options passed in DotAssetCreateOptions
     * @param options
     *
     * @memberof DotAssetService
     */
    create(options: DotAssetCreateOptions): Promise<DotCMSContentlet[]> {
        // Only the success arm is ever *resolved*: the body below `throw`s the error array, which is
        // why every consumer reads it from a `.catch`. The old
        // `Promise<DotCMSContentlet[] | DotHttpErrorResponse[]>` described a value that never arrives.
        //
        // NOTE: the per-request `.catch((e) => e)` below resolves a rejected fetch *as* its error, so
        // an element of `response` can be an `Error` rather than a `Response` and `res.json()` then
        // throws — the consumer's `.catch` receives a `TypeError` instead of `DotHttpErrorResponse[]`.
        // Pre-existing, and a behaviour fix rather than a typing one.
        const promises: Promise<Response>[] = [];
        let filesCreated = 1;
        options.files.map((file: DotCMSTempFile) => {
            const data = {
                contentlet: {
                    baseType: 'dotAsset',
                    asset: file.id,
                    hostFolder: options.folder,
                    indexPolicy: 'WAIT_FOR'
                }
            };

            promises.push(
                fetch(options.url, {
                    method: 'PUT',
                    headers: {
                        Origin: window.location.hostname,
                        'Content-Type': 'application/json;charset=UTF-8'
                    },
                    body: JSON.stringify(data)
                })
                    .then((response: Response) => {
                        options.updateCallback(filesCreated++);
                        return response;
                    })
                    .catch((e) => e)
            );
        });

        return Promise.all(promises).then(async (response: Response[]) => {
            const errors: DotHttpErrorResponse[] = [];
            const data: DotCMSContentlet[] = [];
            for (const res of response) {
                const responseData = await res.json();
                data.push(responseData.entity);
                if (res.status !== 200) {
                    let message = '';
                    try {
                        message = responseData.message || responseData.errors[0].message;
                    } catch {
                        message = fallbackErrorMessages[res.status] ?? '';
                    }
                    errors.push({
                        message: message,
                        status: res.status
                    });
                }
            }

            if (errors.length) {
                throw errors;
            } else {
                return data;
            }
        });
    }
}
