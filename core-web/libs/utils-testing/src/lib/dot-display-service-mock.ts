import { Subject, Observable } from 'rxjs';

import { Injectable } from '@angular/core';

import { DotMessage } from '@dotcms/dotcms-models';

@Injectable()
export class DotMessageDisplayServiceMock {
    messages$: Subject<DotMessage> = new Subject<DotMessage>();

    messages(): Observable<DotMessage> {
        return this.messages$.asObservable();
    }

    push(_message: DotMessage): void {
        /* noop */
    }

    unsubscribe(): void {
        /* noop */
    }
}
