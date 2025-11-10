import { Observable, of } from 'rxjs';

import { Injectable } from '@angular/core';

@Injectable()
export class MockDotHttpErrorManagerService {
    public handle(): Observable<unknown> {
        return of(null);
    }
}
