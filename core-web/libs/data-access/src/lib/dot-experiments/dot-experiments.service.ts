import { Observable, of } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { catchError, pluck } from 'rxjs/operators';

import {
    DotExperiment,
    DotExperimentResults,
    DotExperimentStatus,
    Goals,
    GoalsLevels,
    HealthStatusTypes,
    RangeOfDateAndTime,
    TrafficProportion,
    DotCMSResponse
} from '@dotcms/dotcms-models';

const API_ENDPOINT = '/api/v1/experiments';

interface DotCMSResponseExperiment<T> extends DotCMSResponse<T> {
    entity: T;
}

@Injectable()
export class DotExperimentsService {
    private readonly http = inject(HttpClient);

    /**
     * returns the connection status with the infrastructure of experiments
     * @returns Observable<HealthStatusTypes>
     * @memberof DotExperimentsService
     */
    healthCheck(): Observable<HealthStatusTypes> {
        return this.http
            .get<
                DotCMSResponseExperiment<{ healthy: HealthStatusTypes }>
            >('/api/v1/experiments/health')
            .pipe(pluck('entity', 'health'));
    }
    /**
     * Add a new experiment
     * @param  experiment
     * @returns Observable<DotExperiment>
     * @memberof DotExperimentsService
     */
    add(
        experiment: Pick<DotExperiment, 'pageId' | 'name' | 'description'>
    ): Observable<DotExperiment> {
        return this.http
            .post<DotCMSResponseExperiment<DotExperiment>>(API_ENDPOINT, experiment)
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
            .get<DotCMSResponseExperiment<DotExperiment[]>>(`${API_ENDPOINT}?pageId=${pageId}`)
            .pipe(pluck('entity'));
    }

    /**
     * Get an array of experiments of a pageId filter by status
     * @param {string} pageId
     * @param {DotExperimentStatus} status
     * @returns Observable<DotExperiment[]>
     * @memberof DotExperimentsService
     */
    getByStatus(pageId: string, status: DotExperimentStatus): Observable<DotExperiment[]> {
        return this.http
            .get<
                DotCMSResponseExperiment<DotExperiment[]>
            >(`${API_ENDPOINT}?pageId=${pageId}&status=${status}`)
            .pipe(pluck('entity'));
    }

    /**
     * Get details of an experiment
     * @param {string} experimentId
     * @returns Observable<DotExperiment | undefined>
     * @memberof DotExperimentsService
     */
    getById(experimentId: string | undefined): Observable<DotExperiment | undefined> {
        if (!experimentId) {
            return of(undefined);
        }

        return this.http
            .get<DotCMSResponseExperiment<DotExperiment>>(`${API_ENDPOINT}/${experimentId}`)
            .pipe(pluck('entity'))
            .pipe(catchError(() => of(undefined)));
    }

    /**
     * Get results of an experiment
     * @param {string} experimentId
     * @returns Observable<DotExperiment>
     * @memberof DotExperimentsService
     */
    getResults(experimentId: string): Observable<DotExperimentResults> {
        return this.http
            .get<
                DotCMSResponseExperiment<DotExperimentResults>
            >(`${API_ENDPOINT}/${experimentId}/results`)
            .pipe(pluck('entity'));
    }

    /**
     * Archive an experiment with its experimentId
     * @param {string} experimentId
     * @returns Observable<DotExperiment>
     * @memberof DotExperimentsService
     */
    archive(experimentId: string): Observable<DotExperiment> {
        return this.http
            .put<
                DotCMSResponseExperiment<DotExperiment>
            >(`${API_ENDPOINT}/${experimentId}/_archive`, {})
            .pipe(pluck('entity'));
    }

    /**
     * Delete an experiment with its experimentId
     * @param {string} experimentId
     * @returns Observable<string | DotExperiment>
     * @memberof DotExperimentsService
     */
    delete(experimentId: string): Observable<string | DotExperiment> {
        return this.http
            .delete<DotCMSResponseExperiment<DotExperiment>>(`${API_ENDPOINT}/${experimentId}`)
            .pipe(pluck('entity'));
    }

    /**
     * Start experiment
     * @param {string} experimentId
     * @returns Observable<DotExperiment>
     * @memberof DotExperimentsService
     */
    start(experimentId: string): Observable<DotExperiment> {
        return this.http
            .post<
                DotCMSResponseExperiment<DotExperiment>
            >(`${API_ENDPOINT}/${experimentId}/_start`, {})
            .pipe(pluck('entity'));
    }

    /**
     * Stop experiment
     * @param {string} experimentId
     * @returns Observable<DotExperiment>
     * @memberof DotExperimentsService
     */
    stop(experimentId: string): Observable<DotExperiment> {
        return this.http
            .post<
                DotCMSResponseExperiment<DotExperiment>
            >(`${API_ENDPOINT}/${experimentId}/_end`, {})
            .pipe(pluck('entity'));
    }

    /**
     * Cancel schedule experiment and set it to draft
     * @param {string} experimentId
     * @returns Observable<DotExperiment>
     * @memberof DotExperimentsService
     */
    cancelSchedule(experimentId: string): Observable<DotExperiment> {
        return this.http
            .post<
                DotCMSResponseExperiment<DotExperiment>
            >(`${API_ENDPOINT}/scheduled/${experimentId}/_cancel`, {})
            .pipe(pluck('entity'));
    }

    /**
     * Add variant to experiment
     * @param  {number} experimentId
     * @param {string} name
     * @returns Observable<DotExperiment>
     * @memberof DotExperimentsService
     */
    addVariant(experimentId: string, name: string): Observable<DotExperiment> {
        return this.http
            .post<DotCMSResponseExperiment<DotExperiment>>(
                `${API_ENDPOINT}/${experimentId}/variants`,
                {
                    description: name
                }
            )
            .pipe(pluck('entity'));
    }

    /**
     * Modify a variant of an experiment
     * @param  {number} experimentId
     * @param {string} variantId
     * @param { description: string } changes
     * @returns Observable<DotExperiment>
     * @memberof DotExperimentsService
     */
    editVariant(
        experimentId: string,
        variantId: string,
        changes: { description: string }
    ): Observable<DotExperiment> {
        return this.http
            .put<
                DotCMSResponseExperiment<DotExperiment>
            >(`${API_ENDPOINT}/${experimentId}/variants/${variantId}`, changes)
            .pipe(pluck('entity'));
    }

    /**
     * Remove variant of experiment
     * @param  {string} experimentId
     * @param {string} variantId
     * @returns Observable<DotExperiment>
     * @memberof DotExperimentsService
     */

    removeVariant(experimentId: string, variantId: string): Observable<DotExperiment> {
        return this.http
            .delete<
                DotCMSResponseExperiment<DotExperiment>
            >(`${API_ENDPOINT}/${experimentId}/variants/${variantId}`)
            .pipe(pluck('entity'));
    }

    /**
     * Promote variant of experiment
     * @param {string} experimentId
     * @param {string} variantId
     * @returns Observable<DotExperiment>
     * @memberof DotExperimentsService
     */
    promoteVariant(experimentId: string, variantId: string): Observable<DotExperiment> {
        return this.http
            .put<
                DotCMSResponseExperiment<DotExperiment>
            >(`/api/v1/experiments/${experimentId}/variants/${variantId}/_promote`, {})
            .pipe(pluck('entity'));
    }

    /**
     * Set a selectedGoal to an experiment
     * @param {string} experimentId
     * @param {Goals} goals
     * @returns Observable<DotExperiment>
     * @memberof DotExperimentsService
     */
    setGoal(experimentId: string, goals: Goals): Observable<DotExperiment> {
        return this.http
            .patch<DotCMSResponseExperiment<DotExperiment>>(`${API_ENDPOINT}/${experimentId}`, {
                goals
            })
            .pipe(pluck('entity'));
    }

    /**
     * Set the description to an experiment
     * @param {string} experimentId
     * @param description
     * @returns Observable<DotExperiment>
     * @memberof DotExperimentsService
     */
    setDescription(experimentId: string, description: string): Observable<DotExperiment> {
        return this.http
            .patch<DotCMSResponseExperiment<DotExperiment>>(`${API_ENDPOINT}/${experimentId}`, {
                description
            })
            .pipe(pluck('entity'));
    }

    /**
     * Set scheduling to an experiment
     * @param {string} experimentId
     * @param {RangeOfDateAndTime | null} scheduling
     * @returns Observable<DotExperiment>
     * @memberof DotExperimentsService
     */
    setScheduling(
        experimentId: string,
        scheduling: RangeOfDateAndTime | null
    ): Observable<DotExperiment> {
        return this.http
            .patch<DotCMSResponseExperiment<DotExperiment>>(`${API_ENDPOINT}/${experimentId}`, {
                scheduling
            })
            .pipe(pluck('entity'));
    }

    /**
     * Set traffic allocation to an experiment
     * @param {string} experimentId
     * @param {number} trafficAllocation
     * @returns Observable<DotExperiment>
     * @memberof DotExperimentsService
     */
    setTrafficAllocation(
        experimentId: string,
        trafficAllocation: number
    ): Observable<DotExperiment> {
        return this.http
            .patch<DotCMSResponseExperiment<DotExperiment>>(`${API_ENDPOINT}/${experimentId}`, {
                trafficAllocation
            })
            .pipe(pluck('entity'));
    }

    /**
     * Set traffic portion to an experiment
     * @param {string} experimentId
     * @param {TrafficProportion} trafficProportion
     * @returns Observable<DotExperiment>
     * @memberof DotExperimentsService
     */
    setTrafficProportion(
        experimentId: string,
        trafficProportion: TrafficProportion
    ): Observable<DotExperiment> {
        return this.http
            .patch<DotCMSResponseExperiment<DotExperiment>>(`${API_ENDPOINT}/${experimentId}`, {
                trafficProportion
            })
            .pipe(pluck('entity'));
    }

    /**
     * Delete a goal of an experiment
     * @returns Observable<DotExperiment>
     * @memberof DotExperimentsService
     * @param {string} experimentId
     * @param {GoalsLevels} goalType
     */
    deleteGoal(experimentId: string, goalType: GoalsLevels): Observable<DotExperiment> {
        return this.http
            .delete<
                DotCMSResponseExperiment<DotExperiment>
            >(`${API_ENDPOINT}/${experimentId}/goals/${goalType}`)
            .pipe(pluck('entity'));
    }
}
