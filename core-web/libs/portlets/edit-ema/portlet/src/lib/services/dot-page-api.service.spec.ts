import { createHttpFactory, HttpMethod, SpectatorHttp } from '@ngneat/spectator';

import { UVE_MODE } from '@dotcms/types';

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
                    mode: UVE_MODE.EDIT,
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
                    mode: UVE_MODE.EDIT,
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
                mode: UVE_MODE.EDIT,
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
                mode: UVE_MODE.EDIT,
                language_id: 'en',
                [PERSONA_KEY]: 'modes.persona.no.persona'
            })
            .subscribe();

        spectator.expectOne(
            '/api/v1/page/render/my-folder/?mode=EDIT_MODE&language_id=en&com.dotmarketing.persona.id=modes.persona.no.persona',
            HttpMethod.GET
        );
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
            spectator.service.get({ ...BASE_PARAMS, mode: UVE_MODE.EDIT }).subscribe();
            spectator.expectOne(`${BASE_URL}&mode=${UVE_MODE.EDIT}`, HttpMethod.GET);

            spectator.service.get({ ...BASE_PARAMS, mode: UVE_MODE.PREVIEW }).subscribe();
            spectator.expectOne(`${BASE_URL}&mode=${UVE_MODE.PREVIEW}`, HttpMethod.GET);

            spectator.service.get({ ...BASE_PARAMS, mode: UVE_MODE.LIVE }).subscribe();
            spectator.expectOne(`${BASE_URL}&mode=${UVE_MODE.LIVE}`, HttpMethod.GET);
        });
    });

    describe('preview', () => {
        it("should request page in preview mode if 'mode' is 'preview'", () => {
            spectator.service
                .get({
                    url: 'test-url',
                    language_id: 'en',
                    [PERSONA_KEY]: 'modes.persona.no.persona',
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
                .get({
                    url: 'test-url',
                    language_id: 'en',
                    [PERSONA_KEY]: 'modes.persona.no.persona',
                    mode: UVE_MODE.LIVE
                })
                .subscribe();

            spectator.expectOne(
                `/api/v1/page/render/test-url?language_id=en&com.dotmarketing.persona.id=modes.persona.no.persona&mode=${UVE_MODE.LIVE}`,
                HttpMethod.GET
            );
        });
    });

    describe('getGraphQLPage', () => {
        it('should get the page using graphql if the client send a query', () => {
            const query = 'query { ... }';
            spectator.service.getGraphQLPage({ query, variables: {} }).subscribe();

            const { request } = spectator.expectOne('/api/v1/graphql', HttpMethod.POST);
            const requestHeaders = request.headers;

            expect(request.body).toEqual({ query, variables: {} });
            expect(requestHeaders.get('dotcachettl')).toBe('0');
            expect(requestHeaders.get('Content-Type')).toEqual('application/json');
        });
    });

    describe('saveContentlet', () => {
        it('should send a PUT request with indexPolicy=WAIT_FOR query param', () => {
            const contentlet = {
                inode: 'test-inode-123',
                title: 'Test Title',
                body: 'Test Content'
            };

            spectator.service.saveContentlet({ contentlet }).subscribe();

            const { request } = spectator.expectOne(
                '/api/v1/workflow/actions/default/fire/EDIT?inode=test-inode-123&indexPolicy=WAIT_FOR',
                HttpMethod.PUT
            );

            expect(request.body).toEqual({ contentlet });
        });
    });

    describe('saveStyleProperties', () => {
        it('should send a POST request with correct payload structure', () => {
            const payload = {
                pageId: 'test-page-123',
                containerIdentifier: 'container-id-456',
                containerUUID: 'container-uuid-789',
                contentletIdentifier: 'contentlet-id-abc',
                styleProperties: {
                    'font-size': '16px',
                    color: '#000000'
                }
            };

            spectator.service.saveStyleProperties(payload).subscribe();

            const { request } = spectator.expectOne(
                '/api/v1/page/test-page-123/content',
                HttpMethod.POST
            );

            expect(request.body).toEqual([
                {
                    identifier: 'container-id-456',
                    uuid: 'container-uuid-789',
                    contentletsId: ['contentlet-id-abc'],
                    styleProperties: {
                        'contentlet-id-abc': {
                            'font-size': '16px',
                            color: '#000000'
                        }
                    }
                }
            ]);
        });
    });
});
