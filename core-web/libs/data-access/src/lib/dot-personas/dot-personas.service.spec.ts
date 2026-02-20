import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { DotPersona } from '@dotcms/dotcms-models';

import { DotPersonasService } from './dot-personas.service';

describe('DotPersonasService', () => {
    let service: DotPersonasService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [provideHttpClient(), provideHttpClientTesting(), DotPersonasService]
        });
        service = TestBed.inject(DotPersonasService);
        httpMock = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
        httpMock.verify();
    });

    it('should get personas', () => {
        const mockPersonas: DotPersona[] = [
            {
                name: 'Test Persona',
                keyTag: 'test',
                identifier: 'test-id',
                personalized: false
            }
        ];

        service.get().subscribe((personas: DotPersona[]) => {
            expect(personas).toEqual(mockPersonas);
        });

        const expectedUrl =
            '/api/content/respectFrontendRoles/false/render/false/query/+contentType:persona +live:true +deleted:false +working:true';
        const req = httpMock.expectOne(expectedUrl);
        expect(req.request.method).toBe('GET');
        req.flush({ contentlets: mockPersonas });
    });
});
