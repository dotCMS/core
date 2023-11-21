import { createHttpFactory, HttpMethod, SpectatorHttp } from '@ngneat/spectator';

import { DotPageApiService } from './dot-page-api.service';

describe('DotPageApiService', () => {
    let spectator: SpectatorHttp<DotPageApiService>;
    const createHttp = createHttpFactory(DotPageApiService);

    beforeEach(() => {
        spectator = createHttp();
    });

    it('should send a GET request to retrieve page data', () => {
        spectator.service.get({ url: 'test-url', language_id: 'en' }).subscribe();

        spectator.expectOne('/api/v1/page/json/test-url?language_id=en', HttpMethod.GET);
    });

    it('should send a POST request to save the data', () => {
        spectator.service
            .save({
                pageContainers: [],
                container: {
                    identifier: 'test',
                    acceptTypes: 'test',
                    uuid: 'test',
                    contentletsId: []
                },
                contentletID: 'test',
                pageID: 'test'
            })
            .subscribe();

        spectator.expectOne('`/api/v1/page/test/content?variantName=DEFAULT', HttpMethod.POST);
    });
});
