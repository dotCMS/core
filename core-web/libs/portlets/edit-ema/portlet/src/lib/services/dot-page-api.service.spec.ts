import { createHttpFactory, HttpMethod, SpectatorHttp } from '@ngneat/spectator';

import { DotPageApiService } from './dot-page-api.service';

describe('DotPageApiService', () => {
    let spectator: SpectatorHttp<DotPageApiService>;
    const createHttp = createHttpFactory(DotPageApiService);

    beforeEach(() => {
        spectator = createHttp();
    });

    describe('with clientHost', () => {
        it('should send a GET request with JSON  to retrieve page data', () => {
            spectator.service
                .get({
                    url: 'test-url',
                    language_id: 'en',
                    'com.dotmarketing.persona.id': 'modes.persona.no.persona',
                    clientHost: 'some-host'
                })
                .subscribe();

            spectator.expectOne(
                '/api/v1/page/json/test-url?language_id=en&com.dotmarketing.persona.id=modes.persona.no.persona&variantName=DEFAULT&depth=0&mode=EDIT_MODE',
                HttpMethod.GET
            );
        });
    });

    describe('without clientHost', () => {
        it('should send a GET request with RENDER and EDIT MODE to retrieve page data', () => {
            spectator.service
                .get({
                    url: 'test-url',
                    language_id: 'en',
                    'com.dotmarketing.persona.id': 'modes.persona.no.persona'
                })
                .subscribe();

            spectator.expectOne(
                '/api/v1/page/render/test-url?language_id=en&com.dotmarketing.persona.id=modes.persona.no.persona&variantName=DEFAULT&depth=0&mode=EDIT_MODE',
                HttpMethod.GET
            );
        });
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
            '/api/v1/page/render/test-url?language_id=en&com.dotmarketing.persona.id=modes.persona.no.persona&variantName=DEFAULT&depth=0&mode=EDIT_MODE',
            HttpMethod.GET
        );
    });

    it('should get the page using graphql', () => {
        const query = 'query { ... }';
        spectator.service.getGraphQLPage(query).subscribe();

        const { request } = spectator.expectOne('/api/v1/graphql', HttpMethod.POST);
        const requestHeaders = request.headers;

        expect(request.body).toEqual({ query });
        expect(requestHeaders.get('dotcachettl')).toBe('0');
        expect(requestHeaders.get('Content-Type')).toEqual('application/json');
    });

    describe('getClientPage', () => {
        const baseParams = {
            url: '///test-url///',
            language_id: 'en',
            'com.dotmarketing.persona.id': 'modes.persona.no.persona'
        };

        it('should get the page using graphql if the client send a query', () => {
            const query = 'query { ... }';
            spectator.service.getClientPage(baseParams, { query }).subscribe();

            const { request } = spectator.expectOne('/api/v1/graphql', HttpMethod.POST);
            const requestHeaders = request.headers;

            expect(request.body).toEqual({ query });
            expect(requestHeaders.get('dotcachettl')).toBe('0');
            expect(requestHeaders.get('Content-Type')).toEqual('application/json');
        });

        it('should get the page using the page api if the client does not send a query', () => {
            spectator.service
                .getClientPage(baseParams, {
                    params: {
                        depth: '1'
                    }
                })
                .subscribe();

            spectator.expectOne(
                '/api/v1/page/render/test-url?language_id=en&com.dotmarketing.persona.id=modes.persona.no.persona&variantName=DEFAULT&depth=1&mode=EDIT_MODE',
                HttpMethod.GET
            );
        });
    });
});
