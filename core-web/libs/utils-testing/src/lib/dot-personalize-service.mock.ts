import { Injectable } from '@angular/core';
import { DotCMSPersonalizedItem } from '@dotcms/dotcms-models';
import { of, Observable } from 'rxjs';

@Injectable()
export class DotPersonalizeServiceMock {
    personalized(): Observable<DotCMSPersonalizedItem[]> {
        return of([]);
    }

    despersonalized(): Observable<string> {
        return of('');
    }
}
