import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map, take } from 'rxjs/operators';

import { DOT_CMS_AUTH_TOKEN, DOT_CMS_BASE_URL } from './dot-cms.config';

const BASE_URL = DOT_CMS_BASE_URL;
const AUTH_TOKEN = DOT_CMS_AUTH_TOKEN;

@Injectable({ providedIn: 'root' })
export class DotCmsUploadService {
    private readonly http = inject(HttpClient);

    private authHeaders(): HttpHeaders {
        return new HttpHeaders({
            Authorization: `Bearer ${AUTH_TOKEN}`
        });
    }

    async uploadImage(file: File): Promise<string> {
        return this.uploadAsset(file);
    }

    async uploadVideo(file: File): Promise<string> {
        return this.uploadAsset(file);
    }

    private async uploadAsset(file: File): Promise<string> {
        const tempId = await this.uploadToTemp(file).pipe(take(1)).toPromise();
        if (tempId === undefined) {
            throw new Error('Temp upload: no value emitted');
        }
        const url = await this.publishAsset(tempId).pipe(take(1)).toPromise();
        if (url === undefined) {
            throw new Error('Publish: no value emitted');
        }
        return url;
    }

    private uploadToTemp(file: File) {
        const formData = new FormData();
        formData.append('file', file);

        return this.http
            .post<{ tempFiles: { id: string }[] }>(`${BASE_URL}/api/v1/temp`, formData, {
                headers: this.authHeaders()
            })
            .pipe(
                map((body) => {
                    const id = body.tempFiles?.[0]?.id;
                    if (!id) throw new Error('Temp upload: missing temp file id');
                    return id;
                })
            );
    }

    private publishAsset(tempId: string) {
        interface PublishBody {
            entity: { results: Array<Record<string, { asset: string }>> };
        }

        return this.http
            .post<PublishBody>(
                `${BASE_URL}/api/v1/workflow/actions/default/fire/PUBLISH`,
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
                {
                    headers: this.authHeaders().set(
                        'Content-Type',
                        'application/json;charset=UTF-8'
                    )
                }
            )
            .pipe(
                map((body) => {
                    const row = body.entity?.results?.[0];
                    if (!row) throw new Error('Publish: missing results');
                    const contentlet = Object.values(row)[0] as { asset: string } | undefined;
                    if (!contentlet?.asset) throw new Error('Publish: missing asset path');
                    return `${BASE_URL}${contentlet.asset}`;
                })
            );
    }
}
