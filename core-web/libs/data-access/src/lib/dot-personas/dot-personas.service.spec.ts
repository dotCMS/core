import { HttpTestingController, HttpClientTestingModule } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { CoreWebService } from '@dotcms/dotcms-js';
import { CoreWebServiceMock, mockDotPersona } from '@dotcms/utils-testing';

import { DotPersonasService } from './dot-personas.service';

describe('DotPersonasService', () => {
    let dotPersonasService: DotPersonasService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                DotPersonasService
            ]
        });
        dotPersonasService = TestBed.inject(DotPersonasService);
        httpMock = TestBed.inject(HttpTestingController);
    });

    it('should get Personas', () => {
        const url = [
            `content/respectFrontendRoles/false/render/false/query/+contentType:persona `,
            `+live:true `,
            `+deleted:false `,
            `+working:true`
        ].join('');

        dotPersonasService.get().subscribe((result) => {
            expect(result).toEqual(Array.of(mockDotPersona));
        });

        const req = httpMock.expectOne(url);
        expect(req.request.method).toBe('GET');
        req.flush({ contentlets: [mockDotPersona] });
    });

    afterEach(() => {
        httpMock.verify();
    });
});
