import { of, Observable } from 'rxjs';

import { DotContainerEntity } from '@dotcms/dotcms-models';

const MOCK_CONTAINERS: DotContainerEntity[] = Array.from(Array(10).keys()).map((number) => ({
    container: { friendlyName: `Container ${number}`, parentPermissionable: { hostname: '' } },
    contentTypes: []
}));

/*[
    {
        container: { friendlyName: 'Container 1', parentPermissionable: { hostname: '' } },
        contentTypes: []
    },
    {
        container: { friendlyName: 'Container 2', parentPermissionable: { hostname: '' } },
        contentTypes: []
    },
    {
        container: { friendlyName: 'Container 3', parentPermissionable: { hostname: '' } },
        contentTypes: []
    }
];
*/

export class DotContainersServiceMock {
    getFiltered(_: string): Observable<DotContainerEntity[] | null> {
        return of(MOCK_CONTAINERS);
    }
}
