import { from as observableFrom, empty as observableEmpty, Subject } from 'rxjs';
import { Observable } from 'rxjs';

import { HttpResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { reduce, mergeMap, catchError, map } from 'rxjs/operators';

import { ApiRoot } from '@dotcms/dotcms-js';
import { HttpCode } from '@dotcms/dotcms-js';
import { CoreWebService, LoggerService } from '@dotcms/dotcms-js';

import { ConditionGroupModel, ConditionModel, ICondition } from './Rule';
import { ServerSideTypeModel } from './ServerSideFieldModel';

// tslint:disable-next-line:no-unused-variable
// const noop = (...arg: any[]) => {};

@Injectable()
export class ConditionService {
    public get error(): Observable<string> {
        return this._error.asObservable();
    }
    private _baseUrl: string;

    private _error: Subject<string> = new Subject<string>();

    constructor(
        apiRoot: ApiRoot,
        private coreWebService: CoreWebService,
        private loggerService: LoggerService
    ) {
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
        return this.coreWebService
            .request({
                url: this._baseUrl + '/' + childPath
            })
            .pipe(
                catchError((err: any, _source: Observable<any>) => {
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
        const add = this.coreWebService
            .request({
                method: 'POST',
                body: json,
                url: this._baseUrl + '/'
            })
            .pipe(
                map((res: HttpResponse<any>) => {
                    const json: any = res;
                    model.key = json.id;

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
