import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { DotRole } from '@dotcms/dotcms-models';
import { MockDotMessageService, mockProcessedRoles } from '@dotcms/utils-testing';

import { DotRolesService } from './dot-roles.service';

import { DotMessageService } from '../dot-messages/dot-messages.service';

const mockRoles: DotRole[] = [
    {
        id: '1',
        name: 'Anonymous User',
        user: false,
        roleKey: 'CMS Anonymous'
    },
    { id: '2', name: 'Test Name', user: true, roleKey: 'anonymous' },
    { id: '2', name: 'Some Role', user: true, roleKey: 'roleKey1' }
];

describe('DotRolesService', () => {
    let dotRolesService: DotRolesService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        const messageServiceMock = new MockDotMessageService({
            'current-user': 'Current User',
            user: 'User'
        });

        TestBed.configureTestingModule({
            providers: [
                provideHttpClient(),
                provideHttpClientTesting(),
                DotRolesService,
                { provide: DotMessageService, useValue: messageServiceMock }
            ]
        });
        dotRolesService = TestBed.inject(DotRolesService);
        httpMock = TestBed.inject(HttpTestingController);
    });

    it('should get Roles', () => {
        const url = '/api/v1/roles/123/rolehierarchyanduserroles?roleHierarchyForAssign=false';
        dotRolesService.get('123', false).subscribe((res) => {
            expect(res).toEqual(mockProcessedRoles);
        });

        const req = httpMock.expectOne(url);
        expect(req.request.method).toBe('GET');
        req.flush({ entity: JSON.parse(JSON.stringify(mockRoles)) });
    });

    it('should search Roles', () => {
        const url = '/api/v1/roles/_search';
        dotRolesService.search().subscribe((res) => {
            expect(res).toEqual(mockProcessedRoles);
        });

        const req = httpMock.expectOne(url);
        expect(req.request.method).toBe('GET');
        req.flush({ entity: JSON.parse(JSON.stringify(mockRoles)) });
    });

    describe('getUsers', () => {
        it('should request the direct grants of a role without a filter', () => {
            dotRolesService.getUsers('r/1').subscribe((users) => {
                expect(users).toEqual([{ userId: 'u-1' }]);
            });

            const req = httpMock.expectOne('/api/v1/roles/r%2F1/users?per_page=500');
            expect(req.request.method).toBe('GET');
            req.flush({ entity: [{ userId: 'u-1' }] });
        });

        it('should send a trimmed, encoded filter when one is given', () => {
            dotRolesService.getUsers('r-1', '  jane & co ').subscribe();

            httpMock
                .expectOne('/api/v1/roles/r-1/users?per_page=500&filter=jane%20%26%20co')
                .flush({ entity: [] });
        });

        it('should omit a blank filter', () => {
            dotRolesService.getUsers('r-1', '   ').subscribe();

            httpMock.expectOne('/api/v1/roles/r-1/users?per_page=500').flush({ entity: [] });
        });
    });

    afterEach(() => {
        httpMock.verify();
    });
});
