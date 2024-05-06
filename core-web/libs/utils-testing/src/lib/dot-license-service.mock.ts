import { Observable, of } from 'rxjs';

import { Injectable } from '@angular/core';

@Injectable()
export class DotLicenseServiceMock {
    isEnterprise(): Observable<boolean> {
        return of(true);
    }
}
