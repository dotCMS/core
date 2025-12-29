import { createHttpFactory, HttpMethod, SpectatorHttp } from '@ngneat/spectator';

import { DotSiteBrowserService } from './dot-site-browser.service';

describe('DotSiteBrowserService', () => {
    let spectator: SpectatorHttp<DotSiteBrowserService>;
    const createHttp = createHttpFactory(DotSiteBrowserService);

    beforeEach(() => (spectator = createHttp()));

    it('should set Site Browser Selected folder', () => {
        spectator.service.setSelectedFolder('/test').subscribe();

        const req = spectator.expectOne('/api/v1/browser/selectedfolder', HttpMethod.PUT);
        expect(req.request.body).toEqual({ path: '/test' });

        req.flush({ entity: {} });
    });
});
