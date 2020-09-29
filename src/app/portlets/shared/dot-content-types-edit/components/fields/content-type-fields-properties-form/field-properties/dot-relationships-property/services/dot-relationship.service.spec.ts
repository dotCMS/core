import { DotRelationshipService } from './dot-relationship.service';
import { HttpTestingController, HttpClientTestingModule } from '@angular/common/http/testing';
import { CoreWebService } from 'dotcms-js';
import { CoreWebServiceMock } from 'projects/dotcms-js/src/lib/core/core-web.service.mock';
import { TestBed, getTestBed } from '@angular/core/testing';

const cardinalities = [
    {
        label: 'Many to many',
        id: 0,
        name: 'MANY_TO_MANY'
    },
    {
        label: 'One to one',
        id: 1,
        name: 'ONE_TO_ONE'
    }
];

describe('DotRelationshipService', () => {
    let dotRelationshipService: DotRelationshipService;
    let injector: TestBed;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                DotRelationshipService
            ]
        });
        injector = getTestBed();
        dotRelationshipService = injector.get(DotRelationshipService);
        httpMock = injector.get(HttpTestingController);
    });

    it('should load cardinalities', () => {
        dotRelationshipService.loadCardinalities().subscribe((res: any) => {
            expect(res).toEqual(cardinalities);
        });

        const req = httpMock.expectOne('v1/relationships/cardinalities');
        expect(req.request.method).toBe('GET');
        req.flush({ entity: cardinalities });
    });

    afterEach(() => {
        httpMock.verify();
    });
});
