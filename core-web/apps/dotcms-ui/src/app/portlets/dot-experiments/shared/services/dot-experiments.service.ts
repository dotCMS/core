import { Injectable } from '@angular/core';
import { DotExperiment, Variant } from '@dotcms/dotcms-models';
import { HttpClient } from '@angular/common/http';
import { pluck } from 'rxjs/operators';
import { DotCMSResponse } from '@dotcms/dotcms-js';
import { Observable } from 'rxjs';

const API_ENDPOINT = '/api/v1/experiments';

@Injectable()
export class DotExperimentsService {
    constructor(private readonly http: HttpClient) {}

    /**
     * Add a new experiment
     * @param  experiment
     * @returns Observable<DotExperiment[]>
     * @memberof DotExperimentsService
     */
    add(
        experiment: Pick<DotExperiment, 'pageId' | 'name' | 'description'>
    ): Observable<DotExperiment> {
        return this.http
            .post<DotCMSResponse<DotExperiment>>(API_ENDPOINT, experiment)
            .pipe(pluck('entity'));
    }

    /**
     * Get an array of experiments of a pageId
     * @param {string} pageId
     * @returns Observable<DotExperiment[]>
     * @memberof DotExperimentsService
     */
    getAll(pageId: string): Observable<DotExperiment[]> {
        return this.http
            .get<DotCMSResponse<DotExperiment[]>>(`${API_ENDPOINT}?pageId=${pageId}`)
            .pipe(pluck('entity'));
    }

    /**
     * Get details of an experiment
     * @param {string} experimentId
     * @returns Observable<DotExperiment>
     * @memberof DotExperimentsService
     */
    getById(experimentId: string): Observable<DotExperiment> {
        return this.http
            .get<DotCMSResponse<DotExperiment>>(`${API_ENDPOINT}/${experimentId}`)
            .pipe(pluck('entity'));
    }

    /**
     * Archive an experiment with its experimentId
     * @param {string} experimentId
     * @returns Observable<DotExperiment[]>
     * @memberof DotExperimentsService
     */
    archive(experimentId: string): Observable<DotExperiment[]> {
        return this.http
            .put<DotCMSResponse<DotExperiment[]>>(`${API_ENDPOINT}/${experimentId}/_archive`, {})
            .pipe(pluck('entity'));
    }

    /**
     * Delete an experiment with its experimentId
     * @param {string} experimentId
     * @returns Observable<string | DotExperiment[]>
     * @memberof DotExperimentsService
     */
    delete(experimentId: string): Observable<string | DotExperiment[]> {
        return this.http
            .delete<DotCMSResponse<DotExperiment[]>>(`${API_ENDPOINT}/${experimentId}`)
            .pipe(pluck('entity'));
    }

    /**
     * Add variant to experiment
     * @param  {number} experimentId
     * @param {Variant} variant
     * @returns Observable<DotExperiment[]>
     * @memberof DotExperimentsService
     */
    addVariant(experimentId: string, variant: Pick<Variant, 'name'>): Observable<DotExperiment> {
        return this.http
            .post<DotCMSResponse<DotExperiment>>(
                `${API_ENDPOINT}/${experimentId}/variants`,
                variant
            )
            .pipe(pluck('entity'));
    }

    /**
     * Add variant to experiment
     * @param  {string} experimentId
     * @param {string} variantId
     * @returns Observable<DotExperiment[]>
     * @memberof DotExperimentsService
     */

    removeVariant(experimentId: string, variantId: string): Observable<DotExperiment> {
        return this.http
            .delete<DotCMSResponse<DotExperiment>>(
                `${API_ENDPOINT}/${experimentId}/variants/${variantId}`
            )
            .pipe(pluck('entity'));
    }
}
