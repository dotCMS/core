import { createHttpFactory, HttpMethod, SpectatorHttp } from '@openng/spectator';

import { UVE_MODE } from '@dotcms/types';

import { DotPageApiService } from './dot-page-api.service';

import { PERSONA_KEY } from '../../shared/consts';

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
                '/api/v1/page/json/test-url?mode=EDIT_MODE&language_id=en&com.dotmarketing.persona.id=modes.persona.no.persona&depth=0',
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
                '/api/v1/page/render/test-url?mode=EDIT_MODE&language_id=en&com.dotmarketing.persona.id=modes.persona.no.persona&depth=0',
                HttpMethod.GET
            );
        });

        it('should not override an explicit depth passed by the caller', () => {
            spectator.service
                .get({
                    url: 'test-url',
                    mode: UVE_MODE.EDIT,
                    language_id: 'en',
                    [PERSONA_KEY]: 'modes.persona.no.persona',
                    depth: '2'
                })
                .subscribe();

            spectator.expectOne(
                '/api/v1/page/render/test-url?mode=EDIT_MODE&language_id=en&com.dotmarketing.persona.id=modes.persona.no.persona&depth=2',
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
            '/api/v1/page/render/test-url?mode=EDIT_MODE&language_id=en&com.dotmarketing.persona.id=modes.persona.no.persona&depth=0',
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
            '/api/v1/page/render/my-folder/?mode=EDIT_MODE&language_id=en&com.dotmarketing.persona.id=modes.persona.no.persona&depth=0',
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
            spectator.expectOne(`${BASE_URL}&mode=${UVE_MODE.EDIT}&depth=0`, HttpMethod.GET);

            spectator.service.get({ ...BASE_PARAMS, mode: UVE_MODE.PREVIEW }).subscribe();
            spectator.expectOne(`${BASE_URL}&mode=${UVE_MODE.PREVIEW}&depth=0`, HttpMethod.GET);

            spectator.service.get({ ...BASE_PARAMS, mode: UVE_MODE.LIVE }).subscribe();
            spectator.expectOne(`${BASE_URL}&mode=${UVE_MODE.LIVE}&depth=0`, HttpMethod.GET);
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
                `/api/v1/page/render/test-url?language_id=en&com.dotmarketing.persona.id=modes.persona.no.persona&mode=${UVE_MODE.PREVIEW}&depth=0`,
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
                `/api/v1/page/render/test-url?language_id=en&com.dotmarketing.persona.id=modes.persona.no.persona&mode=${UVE_MODE.LIVE}&depth=0`,
                HttpMethod.GET
            );
        });
    });

    describe('getFormIndetifier', () => {
        const FORM_URL = '/api/v1/containers/form/form-id?containerId=container-id';

        it('should return the identifier of the form already placed in containers', () => {
            let identifier: string | undefined;
            spectator.service
                .getFormIndetifier('container-id', 'form-id')
                .subscribe((value) => (identifier = value));

            spectator
                .expectOne(FORM_URL, HttpMethod.GET)
                .flush({ entity: { content: { identifier: 'existing-widget' } } });

            expect(identifier).toBe('existing-widget');
        });

        it('should ask for the form to be placed in containers when it has not been yet', () => {
            let identifier: string | undefined;
            spectator.service
                .getFormIndetifier('container-id', 'form-id')
                .subscribe((value) => (identifier = value));

            spectator
                .expectOne(FORM_URL, HttpMethod.GET)
                .flush(null, { status: 404, statusText: 'Not Found' });
            spectator
                .expectOne(FORM_URL, HttpMethod.POST)
                .flush({ entity: { content: { identifier: 'new-widget' } } });

            expect(identifier).toBe('new-widget');
        });

        it('should report any other failure without asking for the form to be placed', () => {
            let status: number | undefined;
            spectator.service.getFormIndetifier('container-id', 'form-id').subscribe({
                error: (error) => (status = error.status)
            });

            spectator
                .expectOne(FORM_URL, HttpMethod.GET)
                .flush(null, { status: 403, statusText: 'Forbidden' });
            spectator.controller.expectNone({ method: HttpMethod.POST, url: FORM_URL });

            expect(status).toBe(403);
        });

        describe('when another request is still placing the form', () => {
            beforeEach(() => vi.useFakeTimers());
            afterEach(() => vi.useRealTimers());

            it('should ask again after a moment and return the identifier', () => {
                let identifier: string | undefined;
                spectator.service
                    .getFormIndetifier('container-id', 'form-id')
                    .subscribe((value) => (identifier = value));

                spectator
                    .expectOne(FORM_URL, HttpMethod.GET)
                    .flush(null, { status: 404, statusText: 'Not Found' });
                spectator
                    .expectOne(FORM_URL, HttpMethod.POST)
                    .flush(null, { status: 409, statusText: 'Conflict' });

                vi.advanceTimersByTime(1000);

                spectator
                    .expectOne(FORM_URL, HttpMethod.POST)
                    .flush({ entity: { content: { identifier: 'new-widget' } } });

                expect(identifier).toBe('new-widget');
            });

            it('should report the conflict when it is still there after asking again', () => {
                let status: number | undefined;
                spectator.service.getFormIndetifier('container-id', 'form-id').subscribe({
                    error: (error) => (status = error.status)
                });

                spectator
                    .expectOne(FORM_URL, HttpMethod.GET)
                    .flush(null, { status: 404, statusText: 'Not Found' });
                spectator
                    .expectOne(FORM_URL, HttpMethod.POST)
                    .flush(null, { status: 409, statusText: 'Conflict' });

                vi.advanceTimersByTime(1000);

                spectator
                    .expectOne(FORM_URL, HttpMethod.POST)
                    .flush(null, { status: 409, statusText: 'Conflict' });

                expect(status).toBe(409);
            });
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

    describe('saveStyleProperties', () => {
        it('should send a PUT request with correct payload structure', () => {
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
                '/api/v1/page/test-page-123/styles',
                HttpMethod.PUT
            );

            expect(request.body).toEqual([
                {
                    identifier: 'container-id-456',
                    uuid: 'container-uuid-789',
                    'contentlet-id-abc': {
                        'font-size': '16px',
                        color: '#000000'
                    }
                }
            ]);
        });

        it('should include personaTag in the payload when styling non-default-persona content', () => {
            const payload = {
                pageId: 'test-page-123',
                containerIdentifier: 'container-id-456',
                containerUUID: 'container-uuid-789',
                contentletIdentifier: 'contentlet-id-abc',
                styleProperties: {
                    'font-size': '16px'
                },
                personaTag: 'persona.returning_visitor'
            };

            spectator.service.saveStyleProperties(payload).subscribe();

            const { request } = spectator.expectOne(
                '/api/v1/page/test-page-123/styles',
                HttpMethod.PUT
            );

            expect(request.body).toEqual([
                {
                    identifier: 'container-id-456',
                    uuid: 'container-uuid-789',
                    personaTag: 'persona.returning_visitor',
                    'contentlet-id-abc': {
                        'font-size': '16px'
                    }
                }
            ]);
        });
    });
});
