import { Observable } from 'rxjs';

import { Injectable } from '@angular/core';

import { pluck } from 'rxjs/operators';

import { CoreWebService } from '@dotcms/dotcms-js';

export interface DotPushPublishFilter {
    defaultFilter: boolean;
    key: string;
    title: string;
}

@Injectable()
export class DotPushPublishFiltersService {
    constructor(private coreWebService: CoreWebService) {}

    get(): Observable<DotPushPublishFilter[]> {
        return this.coreWebService
            .requestView({
                url: '/api/v1/pushpublish/filters/'
            })
            .pipe(pluck('entity'));
    }
}
