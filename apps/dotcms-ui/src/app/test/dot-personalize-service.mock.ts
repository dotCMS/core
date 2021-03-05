import { Injectable } from '@angular/core';
import { of, Observable } from 'rxjs';
import { DotCMSPersonalizedItem } from '@services/dot-personalize/dot-personalize.service';

@Injectable()
export class DotPersonalizeServiceMock {
    constructor() {}

    personalized(): Observable<DotCMSPersonalizedItem[]> {
        return of([]);
    }

    despersonalized(): Observable<string> {
        return of('');
    }
}
