import { Observable, of } from 'rxjs';

import { DotTimeZone } from '@dotcms/dotcms-js';

export const mockDotTimeZones = [
    {
        id: 'America/Venezuela',
        label: 'Venezuela',
        offset: '240'
    },
    {
        id: 'America/Costa Rica',
        label: 'Costa Rica',
        offset: '360'
    },
    {
        id: 'America/Panama',
        label: 'Panama',
        offset: '300'
    }
];

export class DotcmsConfigServiceMock {
    getTimeZones(): Observable<DotTimeZone[]> {
        return of(mockDotTimeZones);
    }

    getSystemTimeZone(): Observable<DotTimeZone> {
        return of(mockDotTimeZones[1]);
    }
}
