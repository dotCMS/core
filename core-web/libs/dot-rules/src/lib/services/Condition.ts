import { from as observableFrom, empty as observableEmpty, Subject, Observable } from 'rxjs';

import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { reduce, mergeMap, catchError, map } from 'rxjs/operators';

import { ApiRoot, HttpCode, LoggerService } from '@dotcms/dotcms-js';

import { ConditionGroupModel, ConditionModel, ICondition } from './Rule';
import { ServerSideTypeModel } from './ServerSideFieldModel';

// tslint:disable-next-line:no-unused-variable
// const noop = (...arg: any[]) => {};

@Injectable()
export class ConditionService {
    private http = inject(HttpClient);
    private loggerService = inject(LoggerService);

    private _baseUrl: string;

    private _error: Subject<string> = new Subject<string>();

    public get error(): Observable<string> {
        return this._error.asObservable();
    }

    constructor() {
        const apiRoot = inject(ApiRoot);

        this._baseUrl = `/api/v1/sites/${apiRoot.siteId}/ruleengine/conditions`;
    }

    static toJson(condition: ConditionModel): any {
        const json: any = {};
        json.id = condition.key;
        json.conditionlet = condition.type.key;
        json.priority = condition.priority;
        json.operator = condition.operator;
        json.values = condition.parameters;

        return json;
    }

    static fromServerConditionTransformFn(condition: ICondition): ConditionModel {
        let conditionModel: ConditionModel = null;
        try {
            conditionModel = new ConditionModel(condition);
            const values = condition['values'];

            Object.keys(values).forEach((key) => {
                const x = values[key];
                conditionModel.setParameter(key, x.value, x.priority);
                // tslint:disable-next-line:no-console
                console.log('ConditionService', 'setting parameter', key, x);
            });
        } catch (e) {
            // tslint:disable-next-line:no-console
            console.error('Error reading Condition.', e);
            throw e;
        }

        return conditionModel;
    }

    makeRequest(childPath: string): Observable<any> {
        const path = `${this._baseUrl}/${childPath}`;

        return this.http.get<any>(path).pipe(
            catchError((err: HttpErrorResponse, _source: Observable<any>) => {
                if (err && err.status === HttpCode.NOT_FOUND) {
                    this.loggerService.info('Could not retrieve Condition Types: URL not valid.');
                } else if (err) {
                    this.loggerService.info(
                        'Could not retrieve Condition Types.',
                        'response status code: ',
                        err.status,
                        'error:',
                        err
                    );
                }

                return observableEmpty();
            })
        );
    }

    listForGroup(
        group: ConditionGroupModel,
        conditionTypes?: { [key: string]: ServerSideTypeModel }
    ): Observable<ConditionModel[]> {
        return observableFrom(Object.keys(group.conditions)).pipe(
            mergeMap((conditionId) => {
                return this.get(conditionId, conditionTypes);
            }),
            reduce((acc: ConditionModel[], entity: ConditionModel) => {
                acc.push(entity);

                return acc;
            }, [])
        );
    }

    get(
        conditionId: string,
        conditionTypes?: { [key: string]: ServerSideTypeModel }
    ): Observable<ConditionModel> {
        let conditionModelResult: Observable<ICondition>;
        conditionModelResult = this.makeRequest(conditionId);

        return conditionModelResult.pipe(
            map((entity) => {
                entity.id = conditionId;
                entity._type = conditionTypes ? conditionTypes[entity.conditionlet] : null;

                return ConditionService.fromServerConditionTransformFn(entity);
            })
        );
    }

    add(groupId: string, model: ConditionModel): Observable<any> {
        // this.loggerService.info("api.rule-engine.ConditionService", "add", model)
        if (!model.isValid()) {
            throw new Error(`This should be thrown from a checkValid function on the model,
                        and should provide the info needed to make the user aware of the fix.`);
        }

        const json = ConditionService.toJson(model);
        json.owningGroup = groupId;

        const add = this.http.post<any>(`${this._baseUrl}/`, json).pipe(
            map((res: any) => {
                model.key = res.id;

                return model;
            })
        );

        return add.pipe(catchError(this._catchRequestError('add')));
    }

    save(groupId: string, model: ConditionModel): Observable<ConditionModel> {
        this.loggerService.info('api.rule-engine.ConditionService', 'save', model);
        if (!model.isValid()) {
            throw new Error(`This should be thrown from a checkValid function on the model,
                        and should provide the info needed to make the user aware of the fix.`);
        }

        if (!model.isPersisted()) {
            this.add(groupId, model);
        } else {
            const json = ConditionService.toJson(model);
            json.owningGroup = groupId;

            const save = this.http.put<any>(`${this._baseUrl}/${model.key}`, json).pipe(
                map((_res: any) => {
                    return model;
                })
            );

            return save.pipe(catchError(this._catchRequestError('save')));
        }
    }

    remove(model: ConditionModel): Observable<ConditionModel> {
        const remove = this.http.delete<any>(`${this._baseUrl}/${model.key}`).pipe(
            map((_res: any) => {
                return model;
            })
        );

        return remove.pipe(catchError(this._catchRequestError('remove')));
    }

    private _catchRequestError(
        operation: string
    ): (response: HttpErrorResponse, original: Observable<any>) => Observable<any> {
        return (err: HttpErrorResponse): Observable<any> => {
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
