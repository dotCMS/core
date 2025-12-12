import { Observable } from 'rxjs';

import { Injectable, inject } from '@angular/core';

import { map } from 'rxjs/operators';

import { CoreWebService, ResponseView } from '@dotcms/dotcms-js';

export interface DotPushPublishFilter {
    defaultFilter: boolean;
    key: string;
    title: string;
}
@Injectable({
    providedIn: 'root'
})
export class DotPushPublishFiltersService {
    private coreWebService = inject(CoreWebService);

    get(): Observable<DotPushPublishFilter[]> {
        return this.coreWebService
            .requestView<DotPushPublishFilter[]>({
                url: '/api/v1/pushpublish/filters/'
            })
            .pipe(map((x: ResponseView<DotPushPublishFilter[]>) => x?.entity));
    }
}
