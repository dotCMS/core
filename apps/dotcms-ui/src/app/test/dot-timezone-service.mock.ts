import { DotTimeZone } from '@dotcms/dotcms-js';
import { Observable, of } from 'rxjs';

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
    getTimeZone(): Observable<DotTimeZone[]> {
        return of(mockDotTimeZones);
    }
}
