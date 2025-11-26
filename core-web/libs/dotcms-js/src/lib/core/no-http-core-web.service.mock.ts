import { Observable, of } from 'rxjs';

import { HttpResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { delay } from 'rxjs/operators';

import { ResponseView } from './util/response-view';

@Injectable()
export class NoHttpCoreWebServiceMock {
    private data = inject(any);

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
