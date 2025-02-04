import { createHttpFactory, HttpMethod, SpectatorHttp } from '@ngneat/spectator';

import { UVE_MODE } from '@dotcms/uve/types';

import { DotPageApiService } from './dot-page-api.service';

import { PERSONA_KEY } from '../shared/consts';

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
                    mode: 'EDIT_MODE',
                    language_id: 'en',
                    [PERSONA_KEY]: 'modes.persona.no.persona',
                    clientHost: 'some-host'
                })
                .subscribe();

            spectator.expectOne(
                '/api/v1/page/json/test-url?mode=EDIT_MODE&language_id=en&com.dotmarketing.persona.id=modes.persona.no.persona',
                HttpMethod.GET
            );
        });
    });

    describe('without clientHost', () => {
        it('should send a GET request with RENDER and EDIT MODE to retrieve page data', () => {
            spectator.service
                .get({
                    url: 'test-url',
                    mode: 'EDIT_MODE',
                    language_id: 'en',
                    [PERSONA_KEY]: 'modes.persona.no.persona'
                })
                .subscribe();

            spectator.expectOne(
                '/api/v1/page/render/test-url?mode=EDIT_MODE&language_id=en&com.dotmarketing.persona.id=modes.persona.no.persona',
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
                    [PERSONA_KEY]: 'modes.persona.no.persona'
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
                    [PERSONA_KEY]: 'modes.persona.no.persona',
                    variantName: 'i-have-the-high-ground'
                }
            })
            .subscribe();

        spectator.expectOne(
            '/api/v1/page/test/content?variantName=i-have-the-high-ground',
            HttpMethod.POST
        );
    });

    it('should remove all starting and trailing slashes in the url', () => {
        spectator.service
            .get({
                url: '///test-url',
                mode: 'EDIT_MODE',
                language_id: 'en',
                [PERSONA_KEY]: 'modes.persona.no.persona'
            })
            .subscribe();

        spectator.expectOne(
            '/api/v1/page/render/test-url?mode=EDIT_MODE&language_id=en&com.dotmarketing.persona.id=modes.persona.no.persona',
            HttpMethod.GET
        );
    });

    it('should mantain final trailing slash in the url', () => {
        spectator.service
            .get({
                url: '///my-folder///',
                mode: 'EDIT_MODE',
                language_id: 'en',
                [PERSONA_KEY]: 'modes.persona.no.persona'
            })
            .subscribe();

        spectator.expectOne(
            '/api/v1/page/render/my-folder/?mode=EDIT_MODE&language_id=en&com.dotmarketing.persona.id=modes.persona.no.persona',
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

    describe('preview', () => {
        it("should request page in preview mode if 'editorMode' is 'preview'", () => {
            spectator.service
                .get({
                    url: 'test-url',
                    language_id: 'en',
                    [PERSONA_KEY]: 'modes.persona.no.persona',
                    editorMode: UVE_MODE.PREVIEW
                })
                .subscribe();

            spectator.expectOne(
                '/api/v1/page/render/test-url?language_id=en&com.dotmarketing.persona.id=modes.persona.no.persona&mode=preview',
                HttpMethod.GET
            );
        });
    });

    describe('live', () => {
        it("should request page in live mode if 'editorMode' is 'live'", () => {
            spectator.service
                .get({
                    url: 'test-url',
                    language_id: 'en',
                    [PERSONA_KEY]: 'modes.persona.no.persona',
                    editorMode: UVE_MODE.LIVE
                })
                .subscribe();

            spectator.expectOne(
                '/api/v1/page/render/test-url?language_id=en&com.dotmarketing.persona.id=modes.persona.no.persona&mode=live',
                HttpMethod.GET
            );
        });
    });

    describe('getClientPage', () => {
        const baseParams = {
            url: '///test-url',
            mode: 'EDIT_MODE',
            language_id: 'en',
            [PERSONA_KEY]: 'modes.persona.no.persona'
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
                '/api/v1/page/render/test-url?depth=1&mode=EDIT_MODE&language_id=en&com.dotmarketing.persona.id=modes.persona.no.persona',
                HttpMethod.GET
            );
        });
    });
});
