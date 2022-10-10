import { Injectable } from '@angular/core';
import { DotExperiment } from '../models/dot-experiments.model';
import { HttpClient } from '@angular/common/http';
import { pluck } from 'rxjs/operators';
import { DotCMSResponse } from '@dotcms/dotcms-js';
import { Observable } from 'rxjs';

const API_ENDPOINT = '/api/v1/experiments';

@Injectable()
export class DotExperimentsService {
    constructor(private readonly http: HttpClient) {}

    /**
     * Get an array of experiments of a pageId
     * @param pageId
     * @memberof DotExperimentsService
     */
    get(pageId: string): Observable<DotExperiment[]> {
        const URL = `${API_ENDPOINT}?pageId=${pageId}`;

        return this.http.get<DotCMSResponse<DotExperiment[]>>(URL).pipe(pluck('entity'));
    }

    /**
     * Archive an experiment with its experimentId
     * @param experimentId
     * @memberof DotExperimentsService
     */
    archive(experimentId: string): Observable<DotExperiment[]> {
        const URL = `${API_ENDPOINT}/${experimentId}/_archive`;

        return this.http.put<DotCMSResponse<DotExperiment[]>>(URL, {}).pipe(pluck('entity'));
    }

    /**
     * Delete an experiment with its experimenId
     * @param experimentId
     */
    delete(experimentId: string): Observable<string | DotExperiment[]> {
        const URL = `${API_ENDPOINT}/${experimentId}`;

        return this.http.delete<DotCMSResponse<DotExperiment[]>>(URL).pipe(pluck('entity'));
    }
}
