import { from as observableFrom, empty as observableEmpty, Subject } from 'rxjs';
import { Observable } from 'rxjs';

import { HttpResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { reduce, mergeMap, catchError, map, tap } from 'rxjs/operators';

import { ApiRoot } from '@dotcms/dotcms-js';
import { HttpCode } from '@dotcms/dotcms-js';
import { CoreWebService, LoggerService } from '@dotcms/dotcms-js';

import { ConditionGroupModel, IConditionGroup } from '../rule/Rule';

@Injectable()
export class ConditionGroupService {
    private coreWebService = inject(CoreWebService);
    private loggerService = inject(LoggerService);

    public get error(): Observable<string> {
        return this._error.asObservable();
    }
    private _typeName = 'Condition Group';

    private _baseUrl: string;

    private _error: Subject<string> = new Subject<string>();

    constructor() {
        const apiRoot = inject(ApiRoot);

        this._baseUrl = '/api/v1/sites/' + apiRoot.siteId + '/ruleengine/rules';
    }

    static toJson(conditionGroup: ConditionGroupModel): any {
        const json: any = {};
        json.id = conditionGroup.key;
        json.operator = conditionGroup.operator;
        json.priority = conditionGroup.priority;
        json.conditions = conditionGroup.conditions;

        return json;
    }

    static toJsonList(models: { [key: string]: ConditionGroupModel }): any {
        const list = {};
        Object.keys(models).forEach((key) => {
            list[key] = ConditionGroupService.toJson(models[key]);
        });

        return list;
    }

    makeRequest(path: string): Observable<any> {
        return this.coreWebService
            .request({
                url: path
            })
            .pipe(
                map((res: HttpResponse<any>) => {
                    const json = res;
                    this.loggerService.info('ConditionGroupService', 'makeRequest-Response', json);

                    return json;
                }),
                catchError((err: any, _source: Observable<any>) => {
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
        let result: Observable<ConditionGroupModel>;
        result = this.makeRequest(this._getPath(ruleKey, key)).pipe(
            map((json: IConditionGroup) => {
                json.id = key;
                this.loggerService.info(
                    'ConditionGroupService',
                    'creatingConditionGroupFromJson≠≠'
                );

                return new ConditionGroupModel(json);
            })
        );

        return result;
    }

    createConditionGroup(ruleId: string, model: ConditionGroupModel): Observable<any> {
        this.loggerService.info('ConditionGroupService', 'add', model);
        if (!model.isValid()) {
            throw new Error(`This should be thrown from a checkValid function on the model,
                        and should provide the info needed to make the user aware of the fix`);
        }

        const json = ConditionGroupService.toJson(model);
        const path = this._getPath(ruleId);

        const add = this.coreWebService
            .request({
                method: 'POST',
                body: json,
                url: path
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
            const save = this.coreWebService
                .request({
                    method: 'PUT',
                    body: json,
                    url: this._getPath(ruleId, model.key)
                })
                .pipe(
                    tap(() => {
                        return model;
                    })
                );

            return save.pipe(catchError(this._catchRequestError('save')));
        }
    }

    remove(ruleId: string, model: ConditionGroupModel): Observable<ConditionGroupModel> {
        const remove = this.coreWebService
            .request({
                method: 'DELETE',
                url: this._getPath(ruleId, model.key)
            })
            .pipe(
                tap(() => {
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

    private _catchRequestError(operation): Func {
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

type Func = (any) => Observable<any>;
