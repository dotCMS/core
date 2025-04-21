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
                .get('test-url', {
                    mode: UVE_MODE.EDIT,
                    languageId: 'en',
                    personaId: 'modes.persona.no.persona'
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
                .get('test-url', {
                    mode: UVE_MODE.EDIT,
                    languageId: 'en',
                    personaId: 'modes.persona.no.persona'
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
                    variantName: 'DEFAULT'
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
            .get('///test-url', {
                mode: UVE_MODE.EDIT,
                languageId: 'en',
                personaId: 'modes.persona.no.persona'
            })
            .subscribe();

        spectator.expectOne(
            '/api/v1/page/render/test-url?mode=EDIT_MODE&language_id=en&com.dotmarketing.persona.id=modes.persona.no.persona',
            HttpMethod.GET
        );
    });

    it('should mantain final trailing slash in the url', () => {
        spectator.service
            .get('///my-folder///', {
                mode: UVE_MODE.EDIT,
                languageId: 'en',
                personaId: 'modes.persona.no.persona'
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

    describe('editMode', () => {
        const BASE_URL =
            '/api/v1/page/render/test-url?language_id=en&com.dotmarketing.persona.id=modes.persona.no.persona';

        const BASE_PARAMS = {
            url: 'test-url',
            language_id: 'en',
            [PERSONA_KEY]: 'modes.persona.no.persona'
        };

        it('should request the page in the right `UVE_MODE` based on the `mode`', () => {
            spectator.service.get('test-url', { ...BASE_PARAMS, mode: UVE_MODE.EDIT }).subscribe();
            spectator.expectOne(`${BASE_URL}&mode=${UVE_MODE.EDIT}`, HttpMethod.GET);

            spectator.service
                .get('test-url', { ...BASE_PARAMS, mode: UVE_MODE.PREVIEW })
                .subscribe();
            spectator.expectOne(`${BASE_URL}&mode=${UVE_MODE.PREVIEW}`, HttpMethod.GET);

            spectator.service.get('test-url', { ...BASE_PARAMS, mode: UVE_MODE.LIVE }).subscribe();
            spectator.expectOne(`${BASE_URL}&mode=${UVE_MODE.LIVE}`, HttpMethod.GET);
        });
    });

    describe('preview', () => {
        it("should request page in preview mode if 'mode' is 'preview'", () => {
            spectator.service
                .get('test-url', {
                    languageId: 'en',
                    personaId: 'modes.persona.no.persona',
                    mode: UVE_MODE.PREVIEW
                })
                .subscribe();

            spectator.expectOne(
                `/api/v1/page/render/test-url?language_id=en&com.dotmarketing.persona.id=modes.persona.no.persona&mode=${UVE_MODE.PREVIEW}`,
                HttpMethod.GET
            );
        });
    });

    describe('live', () => {
        it("should request page in live mode if 'mode' is 'live'", () => {
            spectator.service
                .get('test-url', {
                    languageId: 'en',
                    personaId: 'modes.persona.no.persona',
                    mode: UVE_MODE.LIVE
                })
                .subscribe();

            spectator.expectOne(
                `/api/v1/page/render/test-url?language_id=en&com.dotmarketing.persona.id=modes.persona.no.persona&mode=${UVE_MODE.LIVE}`,
                HttpMethod.GET
            );
        });
    });

    // describe('getClientPage', () => {
    //     const baseParams = {
    //         url: '///test-url',
    //         mode: UVE_MODE.EDIT,
    //         language_id: 'en',
    //         [PERSONA_KEY]: 'modes.persona.no.persona'
    //     };

    //     it('should get the page using graphql if the client send a query', () => {
    //         const query = 'query { ... }';
    //         spectator.service.getClientPage(baseParams, { query }).subscribe();

    //         const { request } = spectator.expectOne('/api/v1/graphql', HttpMethod.POST);
    //         const requestHeaders = request.headers;

    //         expect(request.body).toEqual({ query });
    //         expect(requestHeaders.get('dotcachettl')).toBe('0');
    //         expect(requestHeaders.get('Content-Type')).toEqual('application/json');
    //     });

    //     it('should get the page using the page api if the client does not send a query', () => {
    //         spectator.service
    //             .getClientPage(baseParams, {
    //                 params: {
    //                     depth: '1'
    //                 }
    //             })
    //             .subscribe();

    //         spectator.expectOne(
    //             '/api/v1/page/render/test-url?depth=1&mode=EDIT_MODE&language_id=en&com.dotmarketing.persona.id=modes.persona.no.persona',
    //             HttpMethod.GET
    //         );
    //     });
    // });
});
