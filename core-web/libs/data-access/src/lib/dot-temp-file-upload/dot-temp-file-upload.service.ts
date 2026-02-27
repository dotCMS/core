import { Observable } from 'rxjs';

import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { catchError, map, take } from 'rxjs/operators';

import { DotCMSTempFile } from '@dotcms/dotcms-models';

import { DotHttpErrorManagerService } from '../dot-http-error-manager/dot-http-error-manager.service';

// Response type for temp file endpoints
interface DotTempFileResponse {
    tempFiles: DotCMSTempFile[];
}

@Injectable()
export class DotTempFileUploadService {
    private http = inject(HttpClient);
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

        return this.http.post<DotTempFileResponse>('/api/v1/temp', formData).pipe(
            map((response) => response.tempFiles),
            catchError((error: HttpErrorResponse) => this.handleError(error))
        );
    }

    private uploadByUrl(file: string): Observable<DotCMSTempFile[] | string> {
        return this.http
            .post<DotTempFileResponse>('/api/v1/temp/byUrl', {
                remoteUrl: file
            })
            .pipe(
                map((response) => response.tempFiles),
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
