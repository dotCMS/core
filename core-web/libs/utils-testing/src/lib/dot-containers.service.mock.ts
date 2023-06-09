import { of, Observable } from 'rxjs';

import { DotContainerEntity } from '@dotcms/dotcms-models';

import { containersMock } from './dot-containers.mock';

export class DotContainersServiceMock {
    getFiltered(_: string): Observable<DotContainerEntity[] | null> {
        return of(containersMock);
    }
}
