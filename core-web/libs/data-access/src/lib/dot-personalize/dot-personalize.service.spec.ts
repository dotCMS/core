import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { DotCMSPersonalizedItem } from '@dotcms/dotcms-models';

import { DotPersonalizeService } from './dot-personalize.service';

import { DotSessionStorageService } from '../dot-session-storage/dot-session-storage.service';

describe('DotPersonalizeService', () => {
    let service: DotPersonalizeService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        const sessionStorageSpy = jasmine.createSpyObj('DotSessionStorageService', [
            'getVariationId'
        ]);
        sessionStorageSpy.getVariationId.and.returnValue(null);

        TestBed.configureTestingModule({
            providers: [
                provideHttpClient(),
                provideHttpClientTesting(),
                DotPersonalizeService,
                { provide: DotSessionStorageService, useValue: sessionStorageSpy }
            ]
        });
        service = TestBed.inject(DotPersonalizeService);
        httpMock = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
        httpMock.verify();
    });

    it('should personalize a page', () => {
        const mockResponse: DotCMSPersonalizedItem[] = [
            { pageId: 'page-id', personaTag: 'persona-tag' }
        ];

        service.personalized('page-id', 'persona-tag').subscribe((response) => {
            expect(response).toEqual(mockResponse);
        });

        const req = httpMock.expectOne('/api/v1/personalization/pagepersonas');
        expect(req.request.method).toBe('POST');
        expect(req.request.body).toEqual({ pageId: 'page-id', personaTag: 'persona-tag' });
        req.flush({ entity: mockResponse });
    });

    it('should despersonalize a page', () => {
        const mockResponse = 'success';

        service.despersonalized('page-id', 'persona-tag').subscribe((response) => {
            expect(response).toEqual(mockResponse);
        });

        const req = httpMock.expectOne(
            '/api/v1/personalization/pagepersonas/page/page-id/personalization/persona-tag'
        );
        expect(req.request.method).toBe('DELETE');
        req.flush({ entity: mockResponse });
    });
});
