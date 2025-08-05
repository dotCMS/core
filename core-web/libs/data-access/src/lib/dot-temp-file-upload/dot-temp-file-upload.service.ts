import { Observable } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { catchError, map, pluck, take } from 'rxjs/operators';

import { CoreWebService } from '@dotcms/dotcms-js';
import { DotCMSTempFile } from '@dotcms/dotcms-models';

import { DotHttpErrorManagerService } from '../dot-http-error-manager/dot-http-error-manager.service';

@Injectable()
export class DotTempFileUploadService {
    private coreWebService = inject(CoreWebService);
    private dotHttpErrorManagerService = inject(DotHttpErrorManagerService);

    /**
     * Upload file to the dotcms temp service
     *
     * @param {(File | string)} file
     * @returns {(Observable<DotCMSTempFile[] | string>)}
     * @memberof DotTempFileUploadService
     */
    upload(file: File | string): Observable<DotCMSTempFile[] | string> {
        if (typeof file === 'string') {
            return this.uploadByUrl(file);
        }

        return this.uploadByFile(file);
    }

    private uploadByFile(file: File): Observable<DotCMSTempFile[] | string> {
        const formData = new FormData();
        formData.append('file', file);

        return this.coreWebService
            .requestView<DotCMSTempFile[]>({
                url: `/api/v1/temp`,
                body: formData,
                headers: { 'Content-Type': 'multipart/form-data' },
                method: 'POST'
            })
            .pipe(
                pluck('tempFiles'),
                catchError((error: HttpErrorResponse) => this.handleError(error))
            );
    }

    private uploadByUrl(file: string): Observable<DotCMSTempFile[] | string> {
        return this.coreWebService
            .requestView<DotCMSTempFile[]>({
                url: `/api/v1/temp/byUrl`,
                body: {
                    remoteUrl: file
                },
                headers: {
                    'Content-Type': 'application/json'
                },
                method: 'POST'
            })
            .pipe(
                pluck('tempFiles'),
                catchError((error: HttpErrorResponse) => this.handleError(error))
            );
    }

    private handleError(error: HttpErrorResponse) {
        return this.dotHttpErrorManagerService.handle(error).pipe(
            take(1),
            map((err) => err.status.toString())
        );
    }
}
