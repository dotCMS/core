import { DotRolesService } from './dot-roles.service';
import {
    BaseRequestOptions,
    ConnectionBackend,
    Http,
    RequestOptions,
    Response,
    ResponseOptions
} from '@angular/http';
import { MockBackend } from '@angular/http/testing';
import { DotRole } from '@models/dot-role/dot-role.model';
import { TestBed } from '@angular/core/testing';
import { CoreWebService } from 'dotcms-js';
import { CoreWebServiceMock } from '../../../../../projects/dotcms-js/src/lib/core/core-web.service.mock';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { DotMessageService } from '@services/dot-message/dot-messages.service';

const mockRoles: DotRole[] = [
    {
        id: '1',
        name: 'Anonymous User',
        user: false,
        roleKey: 'CMS Anonymous'
    },
    { id: '2', name: 'Test Name', user: true, roleKey: 'Test' },
    { id: '2', name: 'Some Role ', user: false, roleKey: 'roleKey1' }
];

export const mockProcessedRoles: DotRole[] = [
    {
        id: '1',
        name: 'Current User',
        user: false,
        roleKey: 'CMS Anonymous'
    },
    { id: '2', name: 'Some Role ', user: false, roleKey: 'roleKey1' }
];

const messageServiceMock = new MockDotMessageService({
    'current-user': 'Current User',
});

describe('DotRolesService', () => {
    let service: DotRolesService;
    let connectionBackend: MockBackend;
    let lastConnection;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                DotRolesService,
                { provide: ConnectionBackend, useClass: MockBackend },
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                { provide: RequestOptions, useClass: BaseRequestOptions },
                Http,
                { provide: DotMessageService, useValue: messageServiceMock }
            ]
        });
        service = TestBed.get(DotRolesService);
        connectionBackend = TestBed.get(ConnectionBackend);
        connectionBackend.connections.subscribe((connection: any) => (lastConnection = connection));
    });

    it('should get Roles', () => {
        let result;
        const url = '/api/v1/roles/123/rolehierarchyanduserroles';

        service.get('123').subscribe(res => {
            result = res;
        });

        lastConnection.mockRespond(
            new Response(
                new ResponseOptions({
                    body: { entity: mockRoles }
                })
            )
        );

        expect(result).toEqual(mockProcessedRoles);
        expect(lastConnection.request.method).toBe(0); // 0 is GET method
        expect(lastConnection.request.url).toBe(url);
    });
});
