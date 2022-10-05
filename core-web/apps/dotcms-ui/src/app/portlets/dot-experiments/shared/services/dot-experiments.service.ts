import { Injectable } from '@angular/core';
import { DotExperiment } from '../models/dot-experiments.model';
import { HttpClient } from '@angular/common/http';
import { pluck } from 'rxjs/operators';
import { DotCMSResponse } from '@dotcms/dotcms-js';
import { Observable } from 'rxjs';

@Injectable()
export class DotExperimentsService {
    constructor(private readonly http: HttpClient) {}

    get(pageId: string): Observable<DotExperiment[]> {
        const URL = `/api/v1/experiments?pageId=${pageId}`;

        return this.http.get<DotCMSResponse<DotExperiment[]>>(URL).pipe(pluck('entity'));
    }

    archive(experimentId: string): Observable<DotExperiment[]> {
        const URL = `/api/v1/experiments/${experimentId}/_archive`;

        return this.http.put<DotCMSResponse<DotExperiment[]>>(URL, {}).pipe(pluck('entity'));
    }

    delete(experimentId: string): Observable<string | DotExperiment[]> {
        const URL = `/api/v1/experiments/${experimentId}`;

        return this.http.delete<DotCMSResponse<DotExperiment[]>>(URL).pipe(pluck('entity'));
    }
}
