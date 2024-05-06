import { createHttpFactory, HttpMethod, SpectatorHttp } from '@ngneat/spectator/jest';

import { DotLanguagesService } from '@dotcms/data-access';

describe('DotLanguagesService', () => {
    let spectator: SpectatorHttp<DotLanguagesService>;
    const createHttp = createHttpFactory(DotLanguagesService);

    beforeEach(() => (spectator = createHttp()));

    it('should get Languages', () => {
        spectator.service.get().subscribe();
        spectator.expectOne(`/api/v2/languages`, HttpMethod.GET);
    });

    it('should get Languages by content indode', () => {
        const contentInode = '2';
        spectator.service.get(contentInode).subscribe();
        spectator.expectOne(`/api/v2/languages?contentInode=${contentInode}`, HttpMethod.GET);
    });

    it('should get Languages by pageId', () => {
        const pageIdentifier = '0000-1111-2222-3333';
        spectator.service.getLanguagesUsedPage(pageIdentifier).subscribe();
        spectator.expectOne(`/api/v1/page/${pageIdentifier}/languages`, HttpMethod.GET);
    });
});
