import { DotAppsService } from './dot-apps.service';
import { DOTTestBed } from '../../../test/dot-test-bed';
import { Response, ConnectionBackend, ResponseOptions, RequestMethod } from '@angular/http';
import { MockBackend } from '@angular/http/testing';
import { DotApps } from '@shared/models/dot-apps/dot-apps.model';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';
import { LoginService, CoreWebService } from 'dotcms-js';
import { LoginServiceMock } from '@tests/login-service.mock';
import { mockResponseView } from '@tests/response-view.mock';
import { throwError, of } from 'rxjs';

const mockDotApps = [
    {
        allowExtraParams: true,
        configurationsCount: 0,
        key: 'google-calendar',
        name: 'Google Calendar',
        description: "It's a tool to keep track of your life's events",
        iconUrl: '/dA/d948d85c-3bc8-4d85-b0aa-0e989b9ae235/photo/surfer-profile.jpg'
    },
    {
        allowExtraParams: true,
        configurationsCount: 1,
        key: 'asana',
        name: 'Asana',
        description: "It's asana to keep track of your asana events",
        iconUrl: '/dA/792c7c9f-6b6f-427b-80ff-1643376c9999/photo/mountain-persona.jpg'
    }
];

describe('DotAppsService', () => {
    beforeEach(() => {
        this.injector = DOTTestBed.configureTestingModule({
            providers: [
                DotHttpErrorManagerService,
                DotAppsService,
                {
                    provide: LoginService,
                    useClass: LoginServiceMock
                }
            ]
        });

        this.dotAppsService = this.injector.get(DotAppsService);
        this.dotHttpErrorManagerService = this.injector.get(DotHttpErrorManagerService);
        this.coreWebService = this.injector.get(CoreWebService);
        this.backend = this.injector.get(ConnectionBackend) as MockBackend;
        this.backend.connections.subscribe((connection: any) => {
            this.lastConnection = connection;
        });
    });

    it('should get apps', () => {
        const url = 'v1/apps';

        this.dotAppsService.get().subscribe((apps: DotApps[]) => {
            expect(apps).toEqual(mockDotApps);
        });

        this.lastConnection.mockRespond(
            new Response(
                new ResponseOptions({
                    body: {
                        entity: mockDotApps
                    }
                })
            )
        );

        expect(this.lastConnection.request.method).toBe(RequestMethod.Get);
        expect(this.lastConnection.request.url).toContain(url);
    });

    it('should get filtered app', () => {
        const filter = 'asana';
        const url = `v1/apps?filter=${filter}`;
        this.dotAppsService.get(filter).subscribe((apps: DotApps[]) => {
            expect(apps).toEqual([mockDotApps[1]]);
        });

        this.lastConnection.mockRespond(
            new Response(
                new ResponseOptions({
                    body: {
                        entity: [mockDotApps[1]]
                    }
                })
            )
        );

        expect(this.lastConnection.request.method).toBe(RequestMethod.Get);
        expect(this.lastConnection.request.url).toContain(url);
    });

    it('should throw error on get apps and handle it', () => {
        const error404 = mockResponseView(400);
        spyOn(this.dotHttpErrorManagerService, 'handle').and.callThrough();
        spyOn(this.coreWebService, 'requestView').and.returnValue(throwError(error404));

        this.dotAppsService.get().subscribe();
        expect(this.dotHttpErrorManagerService.handle).toHaveBeenCalledWith(mockResponseView(400));
    });

    it('should get a specific app', () => {
        const appKey = '1';
        const url = `v1/apps/${appKey}`;
        spyOn(this.coreWebService, 'requestView').and.returnValue(
            of({
                entity: mockDotApps[0]
            })
        );

        this.dotAppsService.getConfigurationList(appKey).subscribe((apps: DotApps) => {
            expect(apps).toEqual(mockDotApps[0]);
        });

        expect(this.coreWebService.requestView).toHaveBeenCalledWith({
            method: RequestMethod.Get,
            url
        });
    });

    it('should throw error on get a specific app and handle it', () => {
        const error404 = mockResponseView(400);
        spyOn(this.dotHttpErrorManagerService, 'handle').and.callThrough();
        spyOn(this.coreWebService, 'requestView').and.returnValue(throwError(error404));

        this.dotAppsService.getConfiguration('test').subscribe();
        expect(this.dotHttpErrorManagerService.handle).toHaveBeenCalledWith(mockResponseView(400));
    });

    it('should save a specific configuration from an app', () => {
        const appKey = '1';
        const hostId = 'abc';
        const params = {
            name: { hidden: false, value: 'test' }
        };
        const url = `v1/apps/${appKey}/${hostId}`;

        this.dotAppsService
            .saveSiteConfiguration(appKey, hostId, params)
            .subscribe((response: string) => {
                expect(response).toEqual('ok');
            });

        this.lastConnection.mockRespond(
            new Response(
                new ResponseOptions({
                    body: {
                        entity: 'ok'
                    }
                })
            )
        );

        expect(this.lastConnection.request.method).toBe(RequestMethod.Post);
        expect(this.lastConnection.request._body).toEqual(params);
        expect(this.lastConnection.request.url).toBe(url);
    });

    it('should throw error on Save a specific app and handle it', () => {
        const params = {
            name: { hidden: false, value: 'test' }
        };
        const error404 = mockResponseView(400);
        spyOn(this.dotHttpErrorManagerService, 'handle').and.callThrough();
        spyOn(this.coreWebService, 'requestView').and.returnValue(throwError(error404));

        this.dotAppsService.saveSiteConfiguration('test', '123', params).subscribe();
        expect(this.dotHttpErrorManagerService.handle).toHaveBeenCalledWith(mockResponseView(400));
    });

    it('should delete a specific configuration from an app', () => {
        const appKey = '1';
        const hostId = 'abc';
        const url = `v1/apps/${appKey}/${hostId}`;

        this.dotAppsService.deleteConfiguration(appKey, hostId).subscribe((response: string) => {
            expect(response).toEqual('ok');
        });

        this.lastConnection.mockRespond(
            new Response(
                new ResponseOptions({
                    body: {
                        entity: 'ok'
                    }
                })
            )
        );

        expect(this.lastConnection.request.method).toBe(RequestMethod.Delete);
        expect(this.lastConnection.request.url).toBe(url);
    });

    it('should throw error on delete a specific app and handle it', () => {
        const error404 = mockResponseView(400);
        spyOn(this.dotHttpErrorManagerService, 'handle').and.callThrough();
        spyOn(this.coreWebService, 'requestView').and.returnValue(throwError(error404));

        this.dotAppsService.deleteConfiguration('test', '123').subscribe();
        expect(this.dotHttpErrorManagerService.handle).toHaveBeenCalledWith(mockResponseView(400));
    });

    it('should delete all configurations from an app', () => {
        const appKey = '1';
        const url = `v1/apps/${appKey}`;

        this.dotAppsService.deleteAllConfigurations(appKey).subscribe((response: string) => {
            expect(response).toEqual('ok');
        });

        this.lastConnection.mockRespond(
            new Response(
                new ResponseOptions({
                    body: {
                        entity: 'ok'
                    }
                })
            )
        );

        expect(this.lastConnection.request.method).toBe(RequestMethod.Delete);
        expect(this.lastConnection.request.url).toContain(url);
    });

    it('should throw error on delete all configurations from an app and handle it', () => {
        const error404 = mockResponseView(400);
        spyOn(this.dotHttpErrorManagerService, 'handle').and.callThrough();
        spyOn(this.coreWebService, 'requestView').and.returnValue(throwError(error404));

        this.dotAppsService.deleteAllConfigurations('test').subscribe();
        expect(this.dotHttpErrorManagerService.handle).toHaveBeenCalledWith(mockResponseView(400));
    });
});
