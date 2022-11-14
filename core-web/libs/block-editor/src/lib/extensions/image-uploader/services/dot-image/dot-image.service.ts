import { Injectable } from '@angular/core';
import { uploadFile } from '@dotcms/utils';
import { from, Observable, throwError } from 'rxjs';
import { catchError, pluck, switchMap } from 'rxjs/operators';
import { DotCMSContentlet, DotCMSTempFile } from '@dotcms/dotcms-models';
import { HttpClient } from '@angular/common/http';

export enum FileStatus {
    DOWNLOAD = 'DOWNLOADING',
    IMPORT = 'IMPORTING',
    COMPLETED = 'COMPLETED',
    ERROR = 'ERROR'
}

interface PublishContentProps {
    data: string | File | File[];
    maxSize?: string;
    statusCallback?: (status: FileStatus) => void;
}

@Injectable()
export class DotImageService {
    constructor(private http: HttpClient) {}

    publishContent({
        data,
        maxSize,
        statusCallback = (_status) => {
            /* */
        }
    }: PublishContentProps): Observable<DotCMSContentlet[]> {
        statusCallback(FileStatus.DOWNLOAD);

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

                statusCallback(FileStatus.IMPORT);

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
            }),
            catchError((error) => throwError(error))
        );
    }

    private setTempResource(
        file: string | File | File[],
        maxSize?: string
    ): Observable<DotCMSTempFile | DotCMSTempFile[]> {
        return from(
            uploadFile({
                file,
                progressCallBack: () => {
                    /**/
                },
                maxSize
            })
        );
    }
}
