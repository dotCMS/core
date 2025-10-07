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
import { Injectable, inject } from '@angular/core';

import { map, filter } from 'rxjs/operators';

import { DotCMSResponse, DotRequestOptionsArgs } from './core-web.service';
import { ResponseView } from './util/response-view';

@Injectable()
export class CoreWebServiceMock {
    private _http = inject(HttpClient);

    request<T = any>(options: DotRequestOptionsArgs): Observable<any> {
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
                    (event: HttpEvent<HttpResponse<DotCMSResponse<T>> | any>) =>
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

    requestView<T = any>(options: DotRequestOptionsArgs): Observable<ResponseView<T>> {
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
                    (event: HttpEvent<HttpResponse<DotCMSResponse<T>> | any>) =>
                        event.type === HttpEventType.Response
                ),
                map((resp: HttpResponse<DotCMSResponse<T>>) => {
                    return new ResponseView<T>(resp);
                })
            );
    }

    subscribeTo(httpErrorCode: number): Observable<any> {
        return of({
            error: httpErrorCode
        });
    }
}
