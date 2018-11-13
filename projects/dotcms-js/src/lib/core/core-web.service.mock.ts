import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { RequestOptionsArgs, Http, Request } from '@angular/http';
import { map } from 'rxjs/operators';
import { ResponseView } from './util/response-view';

@Injectable()
export class CoreWebServiceMock {

    constructor(private _http: Http) {}

    request(options: any): Observable<any> {
        return this._http.request(new Request(options));
    }

    public requestView(options: RequestOptionsArgs): Observable<ResponseView> {
        return this._http.request(new Request(<any>options)).pipe(
            map((resp) => {
                return new ResponseView(resp);
            })
        );
    }

    public subscribeTo(httpErrorCode: number): Observable<any> {
        return of({
            error: httpErrorCode
        });
    }
}
