import { from as observableFrom, empty as observableEmpty, Subject, Observable } from 'rxjs';

import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { mergeMap, reduce, catchError, map } from 'rxjs/operators';

import {
    ApiRoot,
    UNKNOWN_RESPONSE_ERROR,
    CwError,
    SERVER_RESPONSE_ERROR,
    NETWORK_CONNECTION_ERROR,
    CLIENTS_ONLY_MESSAGES,
    LoggerService,
    HttpCode
} from '@dotcms/dotcms-js';

import { ActionModel } from './Rule';
import { ServerSideTypeModel } from './ServerSideFieldModel';

@Injectable()
export class ActionService {
    private http = inject(HttpClient);
    private loggerService = inject(LoggerService);

    private _typeName = 'Action';

    private _actionsEndpointUrl: string;

    private _error: Subject<string> = new Subject<string>();

    public get error(): Observable<string> {
        return this._error.asObservable();
    }

    static fromJson(type: ServerSideTypeModel, json: any): ActionModel {
        const ra = new ActionModel(json.key, type, json.priority);
        Object.keys(json.parameters).forEach((key) => {
            const param = json.parameters[key];
            ra.setParameter(key, param.value);
        });

        return ra;
    }

    static toJson(action: ActionModel): any {
        const json: any = {};
        json.actionlet = action.type.key;
        json.priority = action.priority;
        json.parameters = action.parameters;

        return json;
    }

    constructor() {
        const apiRoot = inject(ApiRoot);

        this._actionsEndpointUrl = `/api/v1/sites/${apiRoot.siteId}/ruleengine/actions/`;
    }

    makeRequest(childPath?: string): Observable<any> {
        let path = this._actionsEndpointUrl;
        if (childPath) {
            path = `${path}${childPath}`;
        }

        return this.http.get<any>(path).pipe(
            catchError((err: HttpErrorResponse, _source: Observable<any>) => {
                if (err && err.status === HttpCode.NOT_FOUND) {
                    this.loggerService.error(
                        'Could not retrieve ' + this._typeName + ' : 404 path not valid.',
                        path
                    );
                } else if (err) {
                    this.loggerService.debug(
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

    allAsArray(
        ruleKey: string,
        keys: string[],
        ruleActionTypes?: { [key: string]: ServerSideTypeModel }
    ): Observable<ActionModel[]> {
        return this.all(ruleKey, keys, ruleActionTypes).pipe(
            reduce((acc: ActionModel[], item: ActionModel) => {
                acc.push(item);

                return acc;
            }, [])
        );
    }

    all(
        ruleKey: string,
        keys: string[],
        ruleActionTypes?: { [key: string]: ServerSideTypeModel }
    ): Observable<ActionModel> {
        return observableFrom(keys).pipe(
            mergeMap((groupKey) => {
                return this.get(ruleKey, groupKey, ruleActionTypes);
            })
        );
    }

    get(
        _ruleKey: string,
        key: string,
        ruleActionTypes?: { [key: string]: ServerSideTypeModel }
    ): Observable<ActionModel> {
        return this.makeRequest(key).pipe(
            map((json: any) => {
                json.id = key;
                json.key = key;

                return ActionService.fromJson(ruleActionTypes[json.actionlet], json);
            })
        );
    }

    createRuleAction(ruleId: string, model: ActionModel): Observable<any> {
        this.loggerService.debug('Action', 'add', model);
        if (!model.isValid()) {
            throw new Error(`This should be thrown from a checkValid function on the model,
and should provide the info needed to make the user aware of the fix.`);
        }

        const json = ActionService.toJson(model);
        json.owningRule = ruleId;
        const path = this._getPath(ruleId);

        const add = this.http.post<any>(path, json).pipe(
            map((res: any) => {
                model.key = res.id;

                return model;
            })
        );

        return add.pipe(catchError(this._catchRequestError('add')));
    }

    updateRuleAction(ruleId: string, model: ActionModel): Observable<ActionModel> {
        this.loggerService.debug('actionService', 'save');
        if (!model.isValid()) {
            throw new Error(`This should be thrown from a checkValid function on the model,
                        and should provide the info needed to make the user aware of the fix.`);
        }

        if (!model.isPersisted()) {
            this.createRuleAction(ruleId, model);
        } else {
            const json = ActionService.toJson(model);
            json.owningRule = ruleId;
            const save = this.http.put<any>(this._getPath(ruleId, model.key), json).pipe(
                map((_res: any) => {
                    return model;
                })
            );

            return save.pipe(catchError(this._catchRequestError('save')));
        }
    }

    remove(ruleId, model: ActionModel): Observable<ActionModel> {
        const remove = this.http.delete<any>(this._getPath(ruleId, model.key)).pipe(
            map((_res: any) => {
                return model;
            })
        );

        return remove.pipe(catchError(this._catchRequestError('remove')));
    }

    private _getPath(_ruleKey: string, key?: string): string {
        let p = this._actionsEndpointUrl;
        if (key) {
            p = p + key;
        }

        return p;
    }

    private _catchRequestError(
        _operation
    ): (response: HttpErrorResponse, original: Observable<any>) => Observable<any> {
        return (response: HttpErrorResponse): Observable<any> => {
            if (response) {
                if (response.status === HttpCode.SERVER_ERROR) {
                    if (
                        response.error &&
                        JSON.stringify(response.error).indexOf('ECONNREFUSED') >= 0
                    ) {
                        throw new CwError(
                            NETWORK_CONNECTION_ERROR,
                            CLIENTS_ONLY_MESSAGES[NETWORK_CONNECTION_ERROR]
                        );
                    } else {
                        throw new CwError(
                            SERVER_RESPONSE_ERROR,
                            response.headers.get('error-message')
                        );
                    }
                } else if (response.status === HttpCode.NOT_FOUND) {
                    this.loggerService.error('Could not execute request: 404 path not valid.');
                    throw new CwError(
                        UNKNOWN_RESPONSE_ERROR,
                        response.headers.get('error-message')
                    );
                } else {
                    this.loggerService.debug(
                        'Could not execute request: Response status code: ',
                        response.status,
                        'error:',
                        response
                    );

                    this._error.next(
                        response.error?.error?.replace('dotcms.api.error.forbidden: ', '') ?? ''
                    );

                    throw new CwError(
                        UNKNOWN_RESPONSE_ERROR,
                        response.headers.get('error-message')
                    );
                }
            }

            return null;
        };
    }
}
