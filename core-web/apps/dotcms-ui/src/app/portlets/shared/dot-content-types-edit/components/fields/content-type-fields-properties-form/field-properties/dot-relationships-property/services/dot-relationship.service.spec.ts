/* eslint-disable @typescript-eslint/no-explicit-any */

import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { CoreWebService } from '@dotcms/dotcms-js';
import { CoreWebServiceMock } from '@dotcms/utils-testing';

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
            imports: [HttpClientTestingModule],
            providers: [
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                DotRelationshipService
            ]
        });
        dotRelationshipService = TestBed.inject(DotRelationshipService);
        httpMock = TestBed.inject(HttpTestingController);
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
