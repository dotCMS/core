import { DotCurrentUserService } from './dot-current-user.service';
import {
    ConnectionBackend,
    ResponseOptions,
    Response,
    Http,
    RequestOptions,
    BaseRequestOptions
} from '@angular/http';
import { MockBackend } from '@angular/http/testing';
import { TestBed } from '@angular/core/testing';
import { CoreWebService } from 'dotcms-js';
import { CoreWebServiceMock } from 'projects/dotcms-js/src/lib/core/core-web.service.mock';

describe('DotCurrentUserService', () => {
    let dotCurrentUserService: DotCurrentUserService;
    let backend;
    let lastConnection;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                DotCurrentUserService,
                Http,
                { provide: ConnectionBackend, useClass: MockBackend },
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                { provide: RequestOptions, useClass: BaseRequestOptions }
            ]
        });

        dotCurrentUserService = TestBed.get(DotCurrentUserService);
        backend = TestBed.get(ConnectionBackend);
        backend.connections.subscribe((connection: any) => (lastConnection = connection));
    });

    it('should get logged user', () => {
        const mockCurrentUserResponse = {
            email: 'admin@dotcms.com',
            givenName: 'TEST',
            roleId: 'e7d23sde-5127-45fc-8123-d424fd510e3',
            surnaname: 'User',
            userId: 'testId'
        };
        let currentUser: any;
        dotCurrentUserService.getCurrentUser().subscribe((user: any) => {
            currentUser = user._body;
        });

        lastConnection.mockRespond(
            new Response(
                new ResponseOptions({
                    body: mockCurrentUserResponse
                })
            )
        );
        expect(lastConnection.request.url).toContain('v1/users/current');
        expect(currentUser).toEqual(mockCurrentUserResponse);
    });

    it('should get user has access to specific Portlet', () => {
        let userHasAccess: boolean;
        dotCurrentUserService.hasAccessToPortlet('test').subscribe((hasAccess: boolean) => {
            userHasAccess = hasAccess;
        });

        lastConnection.mockRespond(
            new Response(
                new ResponseOptions({
                    body: {
                        entity: {
                            response: true
                        }
                    }
                })
            )
        );
        expect(lastConnection.request.method).toBe(0); // 0 is GET method
        expect(lastConnection.request.url).toContain('v1/portlet/test/_doesuserhaveaccess');
        expect(userHasAccess).toEqual(true);
    });
});
