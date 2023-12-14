import { createHttpFactory, HttpMethod, SpectatorHttp } from '@ngneat/spectator';

import { DotPageApiService } from './dot-page-api.service';

describe('DotPageApiService', () => {
    let spectator: SpectatorHttp<DotPageApiService>;
    const createHttp = createHttpFactory(DotPageApiService);

    beforeEach(() => {
        spectator = createHttp();
    });

    it('should send a GET request to retrieve page data', () => {
        spectator.service
            .get({ url: 'test-url', language_id: 'en', persona_id: 'modes.persona.no.persona' })
            .subscribe();

        spectator.expectOne(
            '/api/v1/page/json/test-url?language_id=en&com.dotmarketing.persona.id=modes.persona.no.persona',
            HttpMethod.GET
        );
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
                pageID: 'test'
            })
            .subscribe();

        spectator.expectOne('/api/v1/page/test/content', HttpMethod.POST);
    });
});
