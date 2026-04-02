import {
    createHttpFactory,
    HttpMethod,
    mockProvider,
    SpectatorHttp,
    SpyObject
} from '@ngneat/spectator/jest';

import { DotCMSPersonalizedItem } from '@dotcms/dotcms-models';

import { DotPersonalizeService } from './dot-personalize.service';

import { DotSessionStorageService } from '../dot-session-storage/dot-session-storage.service';

describe('DotPersonalizeService', () => {
    let spectator: SpectatorHttp<DotPersonalizeService>;
    let sessionStorageService: SpyObject<DotSessionStorageService>;

    const createHttp = createHttpFactory({
        service: DotPersonalizeService,
        providers: [mockProvider(DotSessionStorageService)]
    });

    beforeEach(() => {
        spectator = createHttp();
        sessionStorageService = spectator.inject(DotSessionStorageService);
        sessionStorageService.getVariationId.mockReturnValue('');
    });

    describe('personalized', () => {
        it('should personalize a page without variant name', () => {
            const mockResponse: DotCMSPersonalizedItem[] = [
                {
                    relationType: 'relation-type',
                    treeOrder: 1,
                    personalization: 'persona-tag',
                    container: 'container-id',
                    contentlet: 'contentlet-id',
                    htmlPage: 'page-id'
                }
            ];

            spectator.service.personalized('page-id', 'persona-tag').subscribe((response) => {
                expect(response).toEqual(mockResponse);
            });

            const req = spectator.expectOne(
                '/api/v1/personalization/pagepersonas',
                HttpMethod.POST
            );
            expect(req.request.body).toEqual({ pageId: 'page-id', personaTag: 'persona-tag' });
            expect(req.request.params.has('variantName')).toBe(false);
            req.flush({ entity: mockResponse });
        });

        it('should personalize a page with variant name when available', () => {
            sessionStorageService.getVariationId.mockReturnValue('test-variant');

            const mockResponse: DotCMSPersonalizedItem[] = [
                {
                    relationType: 'relation-type',
                    treeOrder: 1,
                    personalization: 'persona-tag',
                    container: 'container-id',
                    contentlet: 'contentlet-id',
                    htmlPage: 'page-id'
                }
            ];

            spectator.service.personalized('page-id', 'persona-tag').subscribe((response) => {
                expect(response).toEqual(mockResponse);
            });

            const req = spectator.expectOne(
                '/api/v1/personalization/pagepersonas?variantName=test-variant',
                HttpMethod.POST
            );
            expect(req.request.body).toEqual({ pageId: 'page-id', personaTag: 'persona-tag' });
            expect(req.request.params.get('variantName')).toBe('test-variant');
            req.flush({ entity: mockResponse });
        });
    });

    describe('despersonalized', () => {
        it('should despersonalize a page without variant name', () => {
            const mockResponse = 'success';

            spectator.service.despersonalized('page-id', 'persona-tag').subscribe((response) => {
                expect(response).toEqual(mockResponse);
            });

            const req = spectator.expectOne(
                '/api/v1/personalization/pagepersonas/page/page-id/personalization/persona-tag',
                HttpMethod.DELETE
            );
            expect(req.request.params.has('variantName')).toBe(false);
            req.flush({ entity: mockResponse });
        });

        it('should despersonalize a page with variant name when available', () => {
            sessionStorageService.getVariationId.mockReturnValue('test-variant');

            const mockResponse = 'success';

            spectator.service.despersonalized('page-id', 'persona-tag').subscribe((response) => {
                expect(response).toEqual(mockResponse);
            });

            const req = spectator.expectOne(
                '/api/v1/personalization/pagepersonas/page/page-id/personalization/persona-tag?variantName=test-variant',
                HttpMethod.DELETE
            );
            expect(req.request.params.get('variantName')).toBe('test-variant');
            req.flush({ entity: mockResponse });
        });
    });
});
