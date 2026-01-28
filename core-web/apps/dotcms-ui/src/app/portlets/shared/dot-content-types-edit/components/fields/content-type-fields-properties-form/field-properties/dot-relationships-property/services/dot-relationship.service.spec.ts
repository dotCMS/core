import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { DotRelationshipService } from './dot-relationship.service';

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
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [provideHttpClient(), provideHttpClientTesting(), DotRelationshipService]
        });
        dotRelationshipService = TestBed.inject(DotRelationshipService);
        httpMock = TestBed.inject(HttpTestingController);
    });

    it('should load cardinalities', () => {
        dotRelationshipService.loadCardinalities().subscribe((res) => {
            expect(res).toEqual(cardinalities);
        });

        const req = httpMock.expectOne('/api/v1/relationships/cardinalities');
        expect(req.request.method).toBe('GET');
        req.flush({ entity: cardinalities });
    });

    afterEach(() => {
        httpMock.verify();
    });
});
