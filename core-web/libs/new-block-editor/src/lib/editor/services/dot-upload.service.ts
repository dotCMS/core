import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map, take } from 'rxjs/operators';

import { type DotImageData } from '../extensions/nodes/image.extension';
import { type DotVideoData } from '../extensions/nodes/video.extension';

/** Resolves API asset paths to a browser-usable URL on the current origin. */
function sameOriginAssetUrl(asset: string): string {
    if (asset.startsWith('http://') || asset.startsWith('https://')) {
        return asset;
    }
    return asset.startsWith('/') ? asset : `/${asset}`;
}

export interface UploadedImage {
    src: string;
    data: DotImageData;
}

export interface UploadedVideo {
    src: string;
    data: DotVideoData;
}

@Injectable({ providedIn: 'root' })
export class DotUploadService {
    private readonly http = inject(HttpClient);

    private jsonHeaders(): HttpHeaders {
        return new HttpHeaders({ 'Content-Type': 'application/json;charset=UTF-8' });
    }

    async uploadImage(file: File): Promise<UploadedImage> {
        return this.uploadAsset(file);
    }

    async uploadVideo(file: File): Promise<UploadedVideo> {
        return this.uploadVideoAsset(file);
    }

    private async uploadAsset(file: File): Promise<UploadedImage> {
        const tempId = await this.uploadToTemp(file).pipe(take(1)).toPromise();
        if (tempId === undefined) {
            throw new Error('Temp upload: no value emitted');
        }
        const result = await this.publishImageAsset(tempId).pipe(take(1)).toPromise();
        if (result === undefined) {
            throw new Error('Publish: no value emitted');
        }
        return result;
    }

    private async uploadVideoAsset(file: File): Promise<UploadedVideo> {
        const tempId = await this.uploadToTemp(file).pipe(take(1)).toPromise();
        if (tempId === undefined) {
            throw new Error('Temp upload: no value emitted');
        }
        const result = await this.publishVideoAsset(tempId).pipe(take(1)).toPromise();
        if (result === undefined) {
            throw new Error('Publish: no value emitted');
        }
        return result;
    }

    private uploadToTemp(file: File) {
        const formData = new FormData();
        formData.append('file', file);

        return this.http
            .post<{ tempFiles: { id: string }[] }>('/api/v1/temp', formData, {
                withCredentials: true
            })
            .pipe(
                map((body) => {
                    const id = body.tempFiles?.[0]?.id;
                    if (!id) throw new Error('Temp upload: missing temp file id');
                    return id;
                })
            );
    }

    private publishImageAsset(tempId: string) {
        interface PublishContentlet {
            asset: string;
            identifier: string;
            inode: string;
            languageId: number;
            title: string;
        }
        interface PublishBody {
            entity: { results: Array<Record<string, PublishContentlet>> };
        }

        return this.http
            .post<PublishBody>(
                '/api/v1/workflow/actions/default/fire/PUBLISH',
                {
                    contentlets: [
                        {
                            baseType: 'dotAsset',
                            asset: tempId,
                            hostFolder: '',
                            indexPolicy: 'WAIT_FOR'
                        }
                    ]
                },
                { headers: this.jsonHeaders(), withCredentials: true }
            )
            .pipe(
                map((body) => {
                    const row = body.entity?.results?.[0];
                    if (!row) throw new Error('Publish: missing results');
                    const contentlet = Object.values(row)[0] as PublishContentlet | undefined;
                    if (!contentlet?.asset) throw new Error('Publish: missing asset path');
                    return {
                        src: sameOriginAssetUrl(contentlet.asset),
                        data: {
                            identifier: contentlet.identifier,
                            inode: contentlet.inode,
                            languageId: contentlet.languageId,
                            title: contentlet.title ?? '',
                            asset: contentlet.asset
                        } satisfies DotImageData
                    };
                })
            );
    }

    private publishVideoAsset(tempId: string) {
        interface PublishContentlet {
            asset: string;
            identifier: string;
            inode: string;
            languageId: number;
            title: string;
        }
        interface PublishBody {
            entity: { results: Array<Record<string, PublishContentlet>> };
        }

        return this.http
            .post<PublishBody>(
                '/api/v1/workflow/actions/default/fire/PUBLISH',
                {
                    contentlets: [
                        {
                            baseType: 'dotAsset',
                            asset: tempId,
                            hostFolder: '',
                            indexPolicy: 'WAIT_FOR'
                        }
                    ]
                },
                { headers: this.jsonHeaders(), withCredentials: true }
            )
            .pipe(
                map((body) => {
                    const row = body.entity?.results?.[0];
                    if (!row) throw new Error('Publish: missing results');
                    const contentlet = Object.values(row)[0] as PublishContentlet | undefined;
                    if (!contentlet?.asset) throw new Error('Publish: missing asset path');
                    return {
                        src: sameOriginAssetUrl(contentlet.asset),
                        data: {
                            identifier: contentlet.identifier,
                            inode: contentlet.inode,
                            languageId: contentlet.languageId,
                            title: contentlet.title ?? '',
                            asset: contentlet.asset
                        } satisfies DotVideoData
                    };
                })
            );
    }
}
