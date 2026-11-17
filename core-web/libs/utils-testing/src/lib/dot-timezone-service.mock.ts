import { Observable, of } from 'rxjs';

import { ConfigParams, DotTimeZone } from '@dotcms/dotcms-js';

import { configParams } from './dotcms-config.service.mock';

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
    getConfig(): Observable<ConfigParams> {
        return of(configParams);
    }

    getTimeZones(): Observable<DotTimeZone[]> {
        return of(mockDotTimeZones);
    }

    getSystemTimeZone(): Observable<DotTimeZone> {
        return of(mockDotTimeZones[1]);
    }
}
