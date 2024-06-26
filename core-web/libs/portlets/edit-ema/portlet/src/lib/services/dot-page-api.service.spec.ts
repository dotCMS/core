import { createHttpFactory, HttpMethod, SpectatorHttp } from '@ngneat/spectator';

import { DotPageApiService } from './dot-page-api.service';

describe('DotPageApiService', () => {
    let spectator: SpectatorHttp<DotPageApiService>;
    const createHttp = createHttpFactory(DotPageApiService);

    beforeEach(() => {
        spectator = createHttp();
    });

    it('should send a GET request (with render) to retrieve page data', () => {
        spectator.service
            .get({
                url: 'test-url',
                language_id: 'en',
                'com.dotmarketing.persona.id': 'modes.persona.no.persona'
            })
            .subscribe();

        spectator.expectOne(
            '/api/v1/page/render/test-url?language_id=en&com.dotmarketing.persona.id=modes.persona.no.persona&variantName=DEFAULT&mode=EDIT_MODE',
            HttpMethod.GET
        );
    });

    it('should send a GET request (with json) to retrieve page data', () => {
        spectator.service
            .get({
                url: 'test-url',
                language_id: 'en',
                'com.dotmarketing.persona.id': 'modes.persona.no.persona',
                clientHost: 'some-host'
            })
            .subscribe();

        spectator.expectOne(
            '/api/v1/page/json/test-url?language_id=en&com.dotmarketing.persona.id=modes.persona.no.persona&variantName=DEFAULT&mode=LIVE',
            HttpMethod.GET
        );
    });

    it('should send a POST request to save the data', () => {
        spectator.service
            .save({
                pageContainers: [],
                pageId: 'test',
                params: {
                    url: 'test-url',
                    language_id: 'en',
                    'com.dotmarketing.persona.id': 'modes.persona.no.persona'
                }
            })
            .subscribe();

        spectator.expectOne('/api/v1/page/test/content?variantName=DEFAULT', HttpMethod.POST);
    });

    it('should send a POST request to save the data to a variant', () => {
        spectator.service
            .save({
                pageContainers: [],
                pageId: 'test',
                params: {
                    url: 'test-url',
                    language_id: 'en',
                    'com.dotmarketing.persona.id': 'modes.persona.no.persona',
                    variantName: 'i-have-the-high-ground'
                }
            })
            .subscribe();

        spectator.expectOne(
            '/api/v1/page/test/content?variantName=i-have-the-high-ground',
            HttpMethod.POST
        );
    });

    it('should remove trailing and leading slashes in the url', () => {
        spectator.service
            .get({
                url: '///test-url///',
                language_id: 'en',
                'com.dotmarketing.persona.id': 'modes.persona.no.persona'
            })
            .subscribe();

        spectator.expectOne(
            '/api/v1/page/render/test-url?language_id=en&com.dotmarketing.persona.id=modes.persona.no.persona&variantName=DEFAULT&mode=EDIT_MODE',
            HttpMethod.GET
        );
    });
});
