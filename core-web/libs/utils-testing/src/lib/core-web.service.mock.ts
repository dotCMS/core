import { Observable, of } from 'rxjs';

import {
    HttpClient,
    HttpRequest,
    HttpEvent,
    HttpEventType,
    HttpResponse,
    HttpParams,
    HttpHeaders
} from '@angular/common/http';
import { Injectable } from '@angular/core';

import { map, filter } from 'rxjs/operators';

import { ResponseView, DotCMSResponse, DotRequestOptionsArgs } from '@dotcms/dotcms-js';

@Injectable()
export class CoreWebServiceMock {
    constructor(private _http: HttpClient) {}

    request<T = unknown>(
        options: DotRequestOptionsArgs
    ): Observable<HttpResponse<DotCMSResponse<T>> | DotCMSResponse<T>> {
        if (!options.method) {
            options.method = 'GET';
        }

        const optionsArgs = {
            params: new HttpParams()
        };

        if (options.params) {
            Object.keys(options.params).forEach((key) => {
                optionsArgs.params = optionsArgs.params.set(key, options.params[key]);
            });
        }

        return this._http
            .request(
                new HttpRequest(options.method, options.url, options.body, {
                    params: optionsArgs.params
                })
            )
            .pipe(
                filter(
                    (event: HttpEvent<HttpResponse<DotCMSResponse<T>> | unknown>) =>
                        event.type === HttpEventType.Response
                ),
                map((resp: HttpResponse<DotCMSResponse<T>>) => {
                    try {
                        return resp.body;
                    } catch (error) {
                        return resp;
                    }
                })
            );
    }

    requestView<T = unknown>(options: DotRequestOptionsArgs): Observable<ResponseView<T>> {
        if (!options.method) {
            options.method = 'GET';
        }

        const optionsArgs = {
            headers: new HttpHeaders(),
            params: new HttpParams()
        };

        if (options.params) {
            Object.keys(options.params).forEach((key: string) => {
                optionsArgs.params = optionsArgs.params.set(key, options.params[key]);
            });
        }

        return this._http
            .request(
                new HttpRequest(options.method, options.url, options.body, {
                    params: optionsArgs.params
                })
            )
            .pipe(
                filter(
                    (event: HttpEvent<HttpResponse<DotCMSResponse<T>> | unknown>) =>
                        event.type === HttpEventType.Response
                ),
                map((resp: HttpResponse<DotCMSResponse<T>>) => {
                    return new ResponseView<T>(resp);
                })
            );
    }

    subscribeTo(httpErrorCode: number): Observable<{ error: number }> {
        return of({
            error: httpErrorCode
        });
    }
}
