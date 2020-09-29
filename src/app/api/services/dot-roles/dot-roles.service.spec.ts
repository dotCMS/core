import { DotRolesService } from './dot-roles.service';
import { DotRole } from '@models/dot-role/dot-role.model';
import { TestBed, getTestBed } from '@angular/core/testing';
import { CoreWebService } from 'dotcms-js';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { CoreWebServiceMock } from 'projects/dotcms-js/src/lib/core/core-web.service.mock';
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

const messageServiceMock = new MockDotMessageService({
    'current-user': 'Current User',
    user: 'User'
});

describe('DotRolesService', () => {
    let injector: TestBed;
    let dotRolesService: DotRolesService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [
                DotRolesService,
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                { provide: DotMessageService, useValue: messageServiceMock }
            ]
        });
        injector = getTestBed();
        dotRolesService = injector.get(DotRolesService);
        httpMock = injector.get(HttpTestingController);
    });

    it('should get Roles', () => {
        const url = '/api/v1/roles/123/rolehierarchyanduserroles?roleHierarchyForAssign=false';
        dotRolesService.get('123', false).subscribe((res) => {
            expect(res).toEqual(mockProcessedRoles);
        });

        const req = httpMock.expectOne(url);
        expect(req.request.method).toBe('GET');
        req.flush({ entity: mockRoles });
    });

    afterEach(() => {
        httpMock.verify();
    });
});
