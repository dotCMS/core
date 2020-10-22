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
import { ResponseView } from './util/response-view';
import { LoggerService } from './logger.service';
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

export interface DotCMSResponse<T> {
    contentlets?: T;
    entity?: T;
    errors: string[];
    i18nMessagesMap: { [key: string]: string };
    messages: string[];
    permissions: string[];
}

export interface DotRequestOptionsArgs {
    url: string;
    body?:
        | {
              [key: string]: any;
          }
        | string;
    method?: string;
    params?: {
        [key: string]: any;
    };
    headers?: {
        [key: string]: any;
    };
}

@Injectable()
export class CoreWebService {
    private httpErrosSubjects: Subject<any>[] = [];

    constructor(
        private loggerService: LoggerService,
        private router: Router,
        private http: HttpClient
    ) {}

    request<T>(options: DotRequestOptionsArgs): Observable<HttpResponse<any>> {
        if (!options.method) {
            options.method = 'GET';
        }

        const request = this.getRequestOpts(options);
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
                        this.emitHttpError(response.status);

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
    requestView<T = any>(options: DotRequestOptionsArgs): Observable<ResponseView<T>> {
        if (!options.method) {
            options.method = 'GET';
        }

        let request;

        if (options.body) {
            if (typeof options.body === 'string') {
                request = this.getRequestOpts<string>(options);
            } else {
                request = this.getRequestOpts<{ [key: string]: any }>(options);
            }
        } else {
            request = this.getRequestOpts(options);
        }

        return this.http.request(request).pipe(
            filter(
                (event: HttpEvent<HttpResponse<DotCMSResponse<T>> | any>) =>
                    event.type === HttpEventType.Response
            ),
            map((resp: HttpResponse<DotCMSResponse<T>>) => {
                if (resp.body && resp.body.errors && resp.body.errors.length > 0) {
                    return this.handleRequestViewErrors<T>(resp);
                } else {
                    return new ResponseView<T>(resp);
                }
            }),
            catchError((err: HttpErrorResponse) => {
                this.emitHttpError(err.status);
                return throwError(this.handleResponseHttpErrors(err));
            })
        );
    }

    /**
     * Emit to the subscriber when the request fail
     *
     * @param {number} httpErrorCode
     * @returns {Observable<number>}
     * @memberof CoreWebService
     */
    subscribeToHttpError(httpErrorCode: number): Observable<void> {
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

    private emitHttpError(status: number): void {
        if (this.httpErrosSubjects[status]) {
            this.httpErrosSubjects[status].next();
        }
    }

    private getRequestOpts<T>(options: DotRequestOptionsArgs): HttpRequest<T> {
        const headers = this.getHttpHeaders(options.headers);
        const params = this.getHttpParams(options.params);
        const url = this.getFixedUrl(options.url);
        let body = <T>options.body || null;

        if (
            options.method === 'POST' ||
            options.method === 'PUT' ||
            options.method === 'PATCH' ||
            options.method === 'DELETE'
        ) {
            return new HttpRequest<T>(options.method, url, body, {
                headers,
                params
            });
        }

        const method = <'GET' | 'HEAD' | 'JSONP' | 'OPTIONS'>options.method;
        return new HttpRequest<T>(method, url, {
            headers,
            params
        });
    }

    private getHttpHeaders(headers: { [key: string]: string }): HttpHeaders {
        let httpHeaders = this.getDefaultRequestHeaders();

        if (headers && Object.keys(headers).length) {
            Object.keys(headers).forEach((key) => {
                httpHeaders = httpHeaders.set(key, headers[key]);
            });
        }

        return httpHeaders;
    }

    private getFixedUrl(url: string): string {
        if (url.startsWith('api')) {
            return `/${url}`;
        }

        const [version] = url.split('/');
        if (version.match(/v[1-9]/g)) {
            return `/api/${url}`;
        }

        return url;
    }

    private getHttpParams(params: { [key: string]: any }): HttpParams {
        if (params && Object.keys(params).length) {
            let httpParams = new HttpParams();

            Object.keys(params).forEach((key: string) => {
                httpParams = httpParams.set(key, params[key]);
            });
            return httpParams;
        }

        return null;
    }

    private getDefaultRequestHeaders(): HttpHeaders {
        let headers = new HttpHeaders()
            .set('Accept', '*/*')
            .set('Content-Type', 'application/json');
        return headers;
    }
}
