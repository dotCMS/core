import { from as observableFrom, empty as observableEmpty, Observable, Subject } from 'rxjs';

import { HttpResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { reduce, mergeMap, catchError, map } from 'rxjs/operators';

import { ApiRoot, HttpCode, CoreWebService, LoggerService } from '@dotcms/dotcms-js';

import { ConditionGroupModel, ConditionModel, ICondition, ParameterModel } from './Rule';
import { ServerSideTypeModel } from './ServerSideFieldModel';

interface ConditionJson {
    id: string;
    conditionlet: string;
    priority: number;
    operator: string;
    values: { [key: string]: ParameterModel };
    owningGroup?: string;
}

@Injectable()
export class ConditionService {
    private coreWebService = inject(CoreWebService);
    private loggerService = inject(LoggerService);

    public get error(): Observable<string> {
        return this._error.asObservable();
    }
    private _baseUrl: string;

    private _error: Subject<string> = new Subject<string>();

    constructor() {
        const apiRoot = inject(ApiRoot);

        this._baseUrl = `/api/v1/sites/${apiRoot.siteId}/ruleengine/conditions`;
    }

    static toJson(condition: ConditionModel): ConditionJson {
        return {
            id: condition.key,
            conditionlet: condition.type.key,
            priority: condition.priority,
            operator: condition.operator,
            values: condition.parameters
        };
    }

    static fromServerConditionTransformFn(condition: ICondition): ConditionModel {
        let conditionModel: ConditionModel = null;
        try {
            conditionModel = new ConditionModel(condition);
            const values = condition['values'] as {
                [key: string]: { value: string; priority: number };
            };

            Object.keys(values).forEach((key) => {
                const x = values[key];
                conditionModel.setParameter(key, x.value, x.priority);
            });
        } catch (e) {
            throw e;
        }

        return conditionModel;
    }

    makeRequest(childPath: string): Observable<ICondition> {
        return this.coreWebService
            .request<ICondition>({
                url: this._baseUrl + '/' + childPath
            })
            .pipe(
                catchError((err: { status?: number }) => {
                    if (err && err.status === HttpCode.NOT_FOUND) {
                        this.loggerService.info(
                            'Could not retrieve Condition Types: URL not valid.'
                        );
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
        const conditionModelResult: Observable<ICondition> = this.makeRequest(conditionId);

        return conditionModelResult.pipe(
            map((entity) => {
                entity.id = conditionId;
                entity._type = conditionTypes ? conditionTypes[entity.conditionlet] : null;

                return ConditionService.fromServerConditionTransformFn(entity);
            })
        );
    }

    add(groupId: string, model: ConditionModel): Observable<ConditionModel> {
        if (!model.isValid()) {
            throw new Error(`This should be thrown from a checkValid function on the model,
                        and should provide the info needed to make the user aware of the fix.`);
        }

        const json = ConditionService.toJson(model);
        json.owningGroup = groupId;
        const add = this.coreWebService
            .request<{ id: string }>({
                method: 'POST',
                body: json,
                url: this._baseUrl + '/'
            })
            .pipe(
                map((res) => {
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
            const body = JSON.stringify(json);
            const save = this.coreWebService
                .request({
                    method: 'PUT',
                    body: body,
                    url: this._baseUrl + '/' + model.key
                })
                .pipe(
                    map((_res: HttpResponse<any>) => {
                        return model;
                    })
                );

            return save.pipe(catchError(this._catchRequestError('save')));
        }
    }

    remove(model: ConditionModel): Observable<ConditionModel> {
        const remove = this.coreWebService
            .request({
                method: 'DELETE',
                url: this._baseUrl + '/' + model.key
            })
            .pipe(
                map((_res: HttpResponse<any>) => {
                    return model;
                })
            );

        return remove.pipe(catchError(this._catchRequestError('remove')));
    }

    private _catchRequestError(operation): (any) => Observable<any> {
        return (err: any) => {
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

            this._error.next(err.json().error.replace('dotcms.api.error.forbidden: ', ''));

            return observableEmpty();
        };
    }
}
