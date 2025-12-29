import { from as observableFrom, empty as observableEmpty, Subject, Observable } from 'rxjs';

import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { reduce, mergeMap, catchError, map } from 'rxjs/operators';

import { ApiRoot, HttpCode, LoggerService } from '@dotcms/dotcms-js';

import { ConditionGroupModel, IConditionGroup } from './Rule';

interface ConditionGroupJson {
    id: string;
    operator: string;
    priority: number;
    conditions: Record<string, unknown>;
}

interface ConditionGroupResponse {
    id: string;
}

@Injectable()
export class ConditionGroupService {
    private http = inject(HttpClient);
    private loggerService = inject(LoggerService);

    private _typeName = 'Condition Group';

    private _baseUrl: string;

    private _error: Subject<string> = new Subject<string>();

    public get error(): Observable<string> {
        return this._error.asObservable();
    }

    constructor() {
        const apiRoot = inject(ApiRoot);

        this._baseUrl = `/api/v1/sites/${apiRoot.siteId}/ruleengine/rules`;
    }

    static toJson(conditionGroup: ConditionGroupModel): ConditionGroupJson {
        return {
            id: conditionGroup.key,
            operator: conditionGroup.operator,
            priority: conditionGroup.priority,
            conditions: conditionGroup.conditions
        };
    }

    static toJsonList(models: {
        [key: string]: ConditionGroupModel;
    }): Record<string, ConditionGroupJson> {
        const list: Record<string, ConditionGroupJson> = {};
        Object.keys(models).forEach((key) => {
            list[key] = ConditionGroupService.toJson(models[key]);
        });

        return list;
    }

    makeRequest(path: string): Observable<IConditionGroup> {
        return this.http.get<IConditionGroup>(path).pipe(
            map((res) => {
                this.loggerService.info('ConditionGroupService', 'makeRequest-Response', res);

                return res;
            }),
            catchError((err: HttpErrorResponse, _source: Observable<IConditionGroup>) => {
                if (err && err.status === HttpCode.NOT_FOUND) {
                    this.loggerService.error(
                        'Could not retrieve ' + this._typeName + ' : 404 path not valid.',
                        path
                    );
                } else if (err) {
                    this.loggerService.info(
                        'Could not retrieve' + this._typeName + ': Response status code: ',
                        err.status,
                        'error:',
                        err,
                        path
                    );
                }

                return observableEmpty();
            })
        );
    }

    all(ruleKey: string, keys: string[]): Observable<ConditionGroupModel> {
        return observableFrom(keys).pipe(
            mergeMap((groupKey) => {
                return this.get(ruleKey, groupKey);
            })
        );
    }

    allAsArray(ruleKey: string, keys: string[]): Observable<ConditionGroupModel[]> {
        return this.all(ruleKey, keys).pipe(
            reduce((acc: ConditionGroupModel[], group: ConditionGroupModel) => {
                acc.push(group);

                return acc;
            }, [])
        );
    }

    get(ruleKey: string, key: string): Observable<ConditionGroupModel> {
        return this.makeRequest(this._getPath(ruleKey, key)).pipe(
            map((json: IConditionGroup) => {
                json.id = key;
                this.loggerService.info(
                    'ConditionGroupService',
                    'creatingConditionGroupFromJson≠≠'
                );

                return new ConditionGroupModel(json);
            })
        );
    }

    createConditionGroup(
        ruleId: string,
        model: ConditionGroupModel
    ): Observable<ConditionGroupModel> {
        this.loggerService.info('ConditionGroupService', 'add', model);
        if (!model.isValid()) {
            throw new Error(`This should be thrown from a checkValid function on the model,
                        and should provide the info needed to make the user aware of the fix`);
        }

        const json = ConditionGroupService.toJson(model);
        const path = this._getPath(ruleId);

        const add = this.http.post<ConditionGroupResponse>(path, json).pipe(
            map((res) => {
                model.key = res.id;

                return model;
            })
        );

        return add.pipe(catchError(this._catchRequestError('add')));
    }

    updateConditionGroup(
        ruleId: string,
        model: ConditionGroupModel
    ): Observable<ConditionGroupModel> {
        this.loggerService.info('ConditionGroupService', 'save');
        if (!model.isValid()) {
            throw new Error(`This should be thrown from a checkValid function on the model,
                        and should provide the info needed to make the user aware of the fix.`);
        }

        if (!model.isPersisted()) {
            this.createConditionGroup(ruleId, model);
        } else {
            const json = ConditionGroupService.toJson(model);
            const save = this.http.put<void>(this._getPath(ruleId, model.key), json).pipe(
                map(() => {
                    return model;
                })
            );

            return save.pipe(catchError(this._catchRequestError('save')));
        }
    }

    remove(ruleId: string, model: ConditionGroupModel): Observable<ConditionGroupModel> {
        const remove = this.http.delete<void>(this._getPath(ruleId, model.key)).pipe(
            map(() => {
                return model;
            })
        );

        return remove.pipe(catchError(this._catchRequestError('remove')));
    }

    private _getPath(ruleKey: string, key?: string): string {
        let p = this._baseUrl + '/' + ruleKey + '/conditionGroups/';
        if (key) {
            p = p + key;
        }

        return p;
    }

    private _catchRequestError(
        operation: string
    ): (
        response: HttpErrorResponse,
        original: Observable<ConditionGroupModel>
    ) => Observable<ConditionGroupModel> {
        return (err: HttpErrorResponse): Observable<ConditionGroupModel> => {
            if (err && err.status === HttpCode.NOT_FOUND) {
                this.loggerService.info('Could not ' + operation + ' Condition: URL not valid.');
            } else if (err) {
                this.loggerService.info(
                    'Could not ' + operation + ' Condition.',
                    'response status code: ',
                    err.status,
                    'error:',
                    err
                );
            }

            this._error.next(err.error?.error?.replace('dotcms.api.error.forbidden: ', '') ?? '');

            return observableEmpty();
        };
    }
}
