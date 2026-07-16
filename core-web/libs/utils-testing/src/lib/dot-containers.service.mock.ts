import { of, Observable } from 'rxjs';

import { DotContainer } from '@dotcms/dotcms-models';

import { containersMock } from './dot-containers.mock';

export class DotContainersServiceMock {
    defaultContainer$ = of(null);

    getFiltered(_: string): Observable<DotContainer[] | null> {
        return of(containersMock);
    }
}
