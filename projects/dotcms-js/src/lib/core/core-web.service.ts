import { RequestOptionsArgs, URLSearchParams, RequestMethod } from '@angular/http';
import { Injectable } from '@angular/core';
import { Subject, Observable, throwError } from 'rxjs';
import { map, catchError, filter } from 'rxjs/operators';

import {
    CwError,
    NETWORK_CONNECTION_ERROR,
    UNKNOWN_RESPONSE_ERROR,
    CLIENTS_ONLY_MESSAGES,
    SERVER_RESPONSE_ERROR
} from './util/http-response-util';
import { ApiRoot } from './api-root.service';
import { ResponseView } from './util/response-view';
import { LoggerService } from './logger.service';
import { BrowserUtil } from './browser-util.service';
import { HttpCode } from './util/http-code';
import { Router } from '@angular/router';
import {
    HttpClient,
    HttpRequest,
    HttpHeaders,
    HttpParams,
    HttpResponse,
    HttpEventType,
    HttpEvent,
    HttpErrorResponse
} from '@angular/common/http';

export const RULE_CREATE = 'RULE_CREATE';
export const RULE_DELETE = 'RULE_DELETE';
export const RULE_UPDATE_NAME = 'RULE_UPDATE_NAME';
export const RULE_UPDATE_ENABLED_STATE = 'RULE_UPDATE_ENABLED_STATE';

export const V_RULE_UPDATE_EXPANDED_STATE = 'V_RULE_UPDATE_EXPANDED_STATE';

export const RULE_UPDATE_FIRE_ON = 'RULE_UPDATE_FIRE_ON';

export const RULE_RULE_ACTION_CREATE = 'RULE_RULE_ACTION_CREATE';
export const RULE_RULE_ACTION_DELETE = 'RULE_RULE_ACTION_DELETE';
export const RULE_RULE_ACTION_UPDATE_TYPE = 'RULE_RULE_ACTION_UPDATE_TYPE';
export const RULE_RULE_ACTION_UPDATE_PARAMETER = 'RULE_RULE_ACTION_UPDATE_PARAMETER';

export const RULE_CONDITION_GROUP_UPDATE_OPERATOR = 'RULE_CONDITION_GROUP_UPDATE_OPERATOR';
export const RULE_CONDITION_GROUP_DELETE = 'RULE_CONDITION_GROUP_DELETE';
export const RULE_CONDITION_GROUP_CREATE = 'RULE_CONDITION_GROUP_CREATE';

export const RULE_CONDITION_CREATE = 'RULE_CONDITION_CREATE';
export const RULE_CONDITION_DELETE = 'RULE_CONDITION_DELETE';
export const RULE_CONDITION_UPDATE_TYPE = 'RULE_CONDITION_UPDATE_TYPE';
export const RULE_CONDITION_UPDATE_PARAMETER = 'RULE_CONDITION_UPDATE_PARAMETER';
export const RULE_CONDITION_UPDATE_OPERATOR = 'RULE_CONDITION_UPDATE_OPERATOR';

export interface DotCMSResponse<T> {
    contentlets?: T;
    entity?: T;
    errors: string[];
    i18nMessagesMap: { [key: string]: string };
    messages: string[];
    permissions: string[];
}

export interface RequestOptionsParams {
    headers?: HttpHeaders;
    reportProgress?: boolean;
    params?: HttpParams;
    responseType?: 'arraybuffer' | 'blob' | 'text' | 'json';
    withCredentials?: boolean;
}

@Injectable()
export class CoreWebService {
    private httpErrosSubjects: Subject<any>[] = [];

    constructor(
        private _apiRoot: ApiRoot,
        private loggerService: LoggerService,
        private browserUtil: BrowserUtil,
        private router: Router,
        private http: HttpClient
    ) {}

    request<T>(options: RequestOptionsArgs): Observable<any> {
        const request = this.getRequestOpts<T>(options);
        const source = options.body;

        return this.http.request(request).pipe(
            filter(
                (event: HttpEvent<HttpResponse<DotCMSResponse<T>> | any>) =>
                    event.type === HttpEventType.Response
            ),
            map((resp: HttpResponse<DotCMSResponse<T>>) => {
                // some endpoints have empty body.
                try {
                    return resp.body;
                } catch (error) {
                    return resp;
                }
            }),
            catchError(
                (response: HttpErrorResponse, _original: Observable<any>): Observable<any> => {
                    if (response) {
                        this.handleHttpError(response);
                        if (
                            response.status === HttpCode.SERVER_ERROR ||
                            response.status === HttpCode.FORBIDDEN
                        ) {
                            if (
                                response.statusText &&
                                response.statusText.indexOf('ECONNREFUSED') >= 0
                            ) {
                                throw new CwError(
                                    NETWORK_CONNECTION_ERROR,
                                    CLIENTS_ONLY_MESSAGES[NETWORK_CONNECTION_ERROR],
                                    request,
                                    response,
                                    source
                                );
                            } else {
                                throw new CwError(
                                    SERVER_RESPONSE_ERROR,
                                    response.error.message,
                                    request,
                                    response,
                                    source
                                );
                            }
                        } else if (response.status === HttpCode.NOT_FOUND) {
                            this.loggerService.error(
                                'Could not execute request: 404 path not valid.',
                                options.url
                            );
                            throw new CwError(
                                UNKNOWN_RESPONSE_ERROR,
                                response.headers.get('error-message'),
                                request,
                                response,
                                source
                            );
                        }
                    }
                    return null;
                }
            )
        );
    }

    /**
     * Return a response adapted to the follow json format:
     *
     * <code>
     * {
     *   "errors":[],
     *   "entity":{},
     *   "messages":[],
     *   "i18nMessagesMap":{}
     * }
     * </code>
     *
     * @RequestOptionsArgs options
     * @returns Observable<ResponseView>
     */
    public requestView<T = any>(options: RequestOptionsArgs): Observable<ResponseView<T>> {
        const request = this.getRequestOpts<T>(options);
        return this.http.request(request).pipe(
            filter(
                (event: HttpEvent<HttpResponse<DotCMSResponse<T>> | any>) =>
                    event.type === HttpEventType.Response
            ),
            map((resp: HttpResponse<DotCMSResponse<T>>) => {
                if (resp.body && resp.body.errors && resp.body.errors.length > 0) {
                    return this.handleRequestViewErrors(resp);
                } else {
                    return new ResponseView<T>(resp);
                }
            }),
            catchError((err: HttpErrorResponse) => {
                return throwError(this.handleResponseHttpErrors(err));
            })
        );
    }

    public subscribeTo<T>(httpErrorCode: number): Observable<T> {
        if (!this.httpErrosSubjects[httpErrorCode]) {
            this.httpErrosSubjects[httpErrorCode] = new Subject();
        }

        return this.httpErrosSubjects[httpErrorCode].asObservable();
    }

    private handleRequestViewErrors<T>(resp: HttpResponse<DotCMSResponse<T>>): ResponseView<T> {
        if (resp.status === 401) {
            this.router.navigate(['/public/login']);
        }

        return new ResponseView<T>(resp);
    }

    private handleResponseHttpErrors(resp: HttpErrorResponse): any {
        if (resp.status === 401) {
            this.router.navigate(['/public/login']);
        }

        return resp;
    }

    private handleHttpError(response: HttpErrorResponse): void {
        if (!this.httpErrosSubjects[response.status]) {
            this.httpErrosSubjects[response.status] = new Subject();
        }

        this.httpErrosSubjects[response.status].next(response);
    }

    private getRequestOpts<T>(options: RequestOptionsArgs): HttpRequest<T> {
        const optionsArgs: RequestOptionsParams = {
            headers: new HttpHeaders(),
            params: new HttpParams()
        };

        optionsArgs.headers = this._apiRoot.getDefaultRequestHeaders();
        const tempHeaders = options.headers
            ? options.headers.toJSON()
            : { 'Content-Type': 'application/json' };

        Object.keys(tempHeaders).forEach((key) => {
            optionsArgs.headers = optionsArgs.headers.set(key, tempHeaders[key]);
        });

        const body =
            options.body && typeof options.body !== 'string'
                ? JSON.stringify(options.body)
                : options.body;

        if (options.url.indexOf('://') === -1) {
            options.url = options.url.startsWith('/api')
                ? `${this._apiRoot.baseUrl}${options.url.substr(1)}`
                : `${this._apiRoot.baseUrl}api/${options.url}`;
        }

        if (this.browserUtil.isIE11()) {
            optionsArgs.params = optionsArgs.params.set('timestamp', String(new Date().getTime()));
        }

        if (options.params) {
            optionsArgs.params = this.setHttpParams(
                <URLSearchParams>options.params,
                optionsArgs.params
            );
        }

        if (options.search) {
            optionsArgs.params = this.setHttpParams(
                <URLSearchParams>options.search,
                optionsArgs.params
            );
        }

        return new HttpRequest<T>(RequestMethod[options.method], options.url, body, optionsArgs);
    }

    private setHttpParams(urlParams: URLSearchParams, httpParams: HttpParams): HttpParams {
        if (urlParams.paramsMap) {
            const searchParams = urlParams.toString().split('&');
            searchParams.forEach((paramString: string) => {
                const [key, value] = paramString.split('=');
                httpParams = httpParams.set(key, value);
            });
        } else {
            Object.keys(urlParams).forEach((key: string) => {
                httpParams = httpParams.set(key, urlParams[key]);
            });
        }
        return httpParams;
    }
}
