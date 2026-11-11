import { createHttpFactory, HttpMethod, SpectatorHttp } from '@openng/spectator/vitest';
import { Subject } from 'rxjs';

import { SiteService } from '@dotcms/dotcms-js';

import { DotCDNService } from './dot-cdn.service';

describe('DotCDNService', () => {
    let spectator: SpectatorHttp<DotCDNService>;
    let site$: Subject<{ identifier: string }>;

    const createHttp = createHttpFactory({
        service: DotCDNService,
        providers: [
            {
                provide: SiteService,
                useFactory: () => ({ getCurrentSite: () => site$ })
            }
        ]
    });

    beforeEach(() => {
        site$ = new Subject();
        spectator = createHttp();
    });

    it('requests stats for the given site and completes after the response', () => {
        let completed = false;
        spectator.service
            .requestStats({ hostId: 'site-1', dateFrom: '2026-11-01', dateTo: '2026-11-02' })
            .subscribe({ complete: () => (completed = true) });

        spectator
            .expectOne(
                '/api/v1/dotcdn/stats?hostId=site-1&dateFrom=2026-11-01&dateTo=2026-11-02',
                HttpMethod.GET
            )
            .flush({ entity: {} });

        expect(completed).toBe(true);
    });

    it('purges the URLs once, for the site selected when called', () => {
        spectator.service.purgeCache(['/a']).subscribe();

        site$.next({ identifier: 'site-1' });
        const req = spectator.expectOne('/api/v1/dotcdn', HttpMethod.DELETE);
        expect(req.request.body).toEqual({
            urls: ['/a'],
            invalidateAll: false,
            hostId: 'site-1'
        });
        req.flush({ entity: { success: false } });

        site$.next({ identifier: 'site-2' });
        spectator.controller.expectNone('/api/v1/dotcdn');
    });

    it('purges the whole zone once, not again on a later site switch', () => {
        spectator.service.purgeCacheAll().subscribe();

        site$.next({ identifier: 'site-1' });
        const req = spectator.expectOne('/api/v1/dotcdn', HttpMethod.DELETE);
        expect(req.request.body).toEqual({ urls: [], invalidateAll: true, hostId: 'site-1' });
        req.flush({ entity: { success: true } });

        site$.next({ identifier: 'site-2' });
        spectator.controller.expectNone('/api/v1/dotcdn');
    });
});
