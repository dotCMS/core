import { Injectable } from '@angular/core';
import { uploadBinaryFile } from '@dotcms/utils';
import { from, Observable } from 'rxjs';
import { pluck, switchMap } from 'rxjs/operators';
import { DotCMSContentlet, DotCMSTempFile } from '@dotcms/dotcms-models';
import { HttpClient } from '@angular/common/http';

@Injectable()
export class DotImageService {
    constructor(private http: HttpClient) {}

    publishContent(data: File | File[], maxSize?: string): Observable<DotCMSContentlet[]> {
        return this.setTempResource(data, maxSize).pipe(
            switchMap((response: DotCMSTempFile | DotCMSTempFile[]) => {
                const files = Array.isArray(response) ? response : [response];
                const contentlets = [];
                files.forEach((file: DotCMSTempFile) => {
                    contentlets.push({
                        baseType: 'dotAsset',
                        asset: file.id,
                        hostFolder: '',
                        indexPolicy: 'WAIT_FOR'
                    });
                });
                return this.http
                    .post(
                        '/api/v1/workflow/actions/default/fire/PUBLISH',
                        JSON.stringify({ contentlets }),
                        {
                            headers: {
                                Origin: window.location.hostname,
                                'Content-Type': 'application/json;charset=UTF-8'
                            }
                        }
                    )
                    .pipe(pluck('entity', 'results')) as Observable<DotCMSContentlet[]>;
            })
        );
    }

    private setTempResource(
        data: File | File[],
        maxSize?: string
    ): Observable<DotCMSTempFile | DotCMSTempFile[]> {
        return from(uploadBinaryFile(data, () => {}, maxSize));
    }
}
