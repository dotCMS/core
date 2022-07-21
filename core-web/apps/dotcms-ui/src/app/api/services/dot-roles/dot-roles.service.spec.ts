import { DotRolesService } from './dot-roles.service';
import { DotRole } from '@models/dot-role/dot-role.model';
import { TestBed } from '@angular/core/testing';
import { CoreWebService } from '@dotcms/dotcms-js';
import { MockDotMessageService } from '@dotcms/app/test/dot-message-service.mock';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { CoreWebServiceMock } from '@tests/core-web.service.mock';
import { HttpTestingController, HttpClientTestingModule } from '@angular/common/http/testing';

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

export const mockProcessedRoles: DotRole[] = [
    {
        id: '1',
        name: 'Current User',
        user: false,
        roleKey: 'CMS Anonymous'
    },
    { id: '2', name: 'Some Role (User)', user: true, roleKey: 'roleKey1' }
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
            imports: [HttpClientTestingModule],
            providers: [
                DotRolesService,
                { provide: CoreWebService, useClass: CoreWebServiceMock },
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

    afterEach(() => {
        httpMock.verify();
    });
});
