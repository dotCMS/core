import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { ResponseView } from './util/response-view';
import { Response, ResponseOptions } from '@angular/http';
import { delay } from 'rxjs/operators';

@Injectable()
export class NoHttpCoreWebServiceMock {

    constructor(private data: any) {}

    public requestView(): Observable<ResponseView> {
        return of(new ResponseView(
            new Response(
                new ResponseOptions({
                    body: JSON.stringify({
                        entity: this.data
                    })
                })
            )
        )).pipe(delay(1000));
    }
}
