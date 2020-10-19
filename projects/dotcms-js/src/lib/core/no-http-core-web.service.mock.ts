import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { ResponseView } from './util/response-view';
import { HttpResponse } from '@angular/common/http';
import { delay } from 'rxjs/operators';

@Injectable()
export class NoHttpCoreWebServiceMock {
    constructor(private data: any) {}

    public requestView(): Observable<ResponseView<any>> {
        return of(
            new ResponseView(
                new HttpResponse({
                    body: {
                        entity: this.data,
                        errors: [],
                        i18nMessagesMap: null,
                        messages: [],
                        permissions: []
                    }
                })
            )
        ).pipe(delay(1000));
    }
}
