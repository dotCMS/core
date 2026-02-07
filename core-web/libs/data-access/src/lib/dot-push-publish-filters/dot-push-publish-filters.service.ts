import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map } from 'rxjs/operators';

import { DotCMSResponse } from '@dotcms/dotcms-models';

export interface DotPushPublishFilter {
    defaultFilter: boolean;
    key: string;
    title: string;
}

@Injectable({
    providedIn: 'root'
})
export class DotPushPublishFiltersService {
    private http = inject(HttpClient);

    get(): Observable<DotPushPublishFilter[]> {
        return this.http
            .get<DotCMSResponse<DotPushPublishFilter[]>>('/api/v1/pushpublish/filters/')
            .pipe(map((response) => response.entity));
    }
}
