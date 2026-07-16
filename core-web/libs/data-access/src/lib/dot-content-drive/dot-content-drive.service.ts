import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map } from 'rxjs/operators';

import { DotContentDriveSearchRequest, DotContentDriveSearchResponse } from '@dotcms/dotcms-models';

@Injectable()
export class DotContentDriveService {
    readonly #http = inject(HttpClient);

    search(request: DotContentDriveSearchRequest): Observable<DotContentDriveSearchResponse> {
        return this.#http
            .post<{ entity: DotContentDriveSearchResponse }>('/api/v1/drive/search', request)
            .pipe(map((response: { entity: DotContentDriveSearchResponse }) => response.entity));
    }
}
