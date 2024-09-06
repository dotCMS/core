import { of, Observable } from 'rxjs';

import { Injectable } from '@angular/core';

import { DotCMSPersonalizedItem } from '@dotcms/dotcms-models';

@Injectable()
export class DotPersonalizeServiceMock {
    personalized(): Observable<DotCMSPersonalizedItem[]> {
        return of([]);
    }

    despersonalized(): Observable<string> {
        return of('');
    }
}
