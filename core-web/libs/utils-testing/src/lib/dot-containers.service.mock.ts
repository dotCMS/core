import { of, Observable } from 'rxjs';

import { DotContainer } from '@dotcms/dotcms-models';

import { containersMock } from './dot-containers.mock';

export class DotContainersServiceMock {
    getFiltered(_: string): Observable<DotContainer[] | null> {
        console.log('here, DotContainersServiceMock');
        return of(containersMock);
    }
}
