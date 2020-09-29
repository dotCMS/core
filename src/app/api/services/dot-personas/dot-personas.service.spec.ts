import { DotPersonasService } from './dot-personas.service';
import { mockDotPersona } from '../../../test/dot-persona.mock';
import { TestBed, getTestBed } from '@angular/core/testing';
import { HttpTestingController, HttpClientTestingModule } from '@angular/common/http/testing';
import { CoreWebService } from 'dotcms-js';
import { CoreWebServiceMock } from 'projects/dotcms-js/src/lib/core/core-web.service.mock';

describe('DotPersonasService', () => {
    let injector: TestBed;
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
        injector = getTestBed();
        dotPersonasService = injector.get(DotPersonasService);
        httpMock = injector.get(HttpTestingController);
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
