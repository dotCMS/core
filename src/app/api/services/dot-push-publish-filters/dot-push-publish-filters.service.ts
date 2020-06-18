import { Injectable } from '@angular/core';
import { CoreWebService } from 'dotcms-js';
import { Observable } from 'rxjs';
import { RequestMethod } from '@angular/http';
import { pluck } from 'rxjs/operators';

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
                method: RequestMethod.Get,
                url: '/api/v1/pushpublish/filters/'
            })
            .pipe(pluck('entity'));
    }
}
