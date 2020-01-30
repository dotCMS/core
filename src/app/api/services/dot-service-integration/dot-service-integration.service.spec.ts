import { DotServiceIntegrationService } from './dot-service-integration.service';
import { DOTTestBed } from '../../../test/dot-test-bed';
import { Response, ConnectionBackend, ResponseOptions, RequestMethod } from '@angular/http';
import { MockBackend } from '@angular/http/testing';
import { DotServiceIntegration } from '@shared/models/dot-service-integration/dot-service-integration.model';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';
import { LoginService, CoreWebService } from 'dotcms-js';
import { LoginServiceMock } from '@tests/login-service.mock';
import { mockResponseView } from '@tests/response-view.mock';
import { throwError, of } from 'rxjs';

const mockDotServiceIntegration = [
    {
        configurationsCount: 0,
        key: 'google-calendar',
        name: 'Google Calendar',
        description: "It's a tool to keep track of your life's events",
        iconUrl: '/dA/d948d85c-3bc8-4d85-b0aa-0e989b9ae235/photo/surfer-profile.jpg'
    },
    {
        configurationsCount: 1,
        key: 'asana',
        name: 'Asana',
        description: "It's asana to keep track of your asana events",
        iconUrl: '/dA/792c7c9f-6b6f-427b-80ff-1643376c9999/photo/mountain-persona.jpg'
    }
];

describe('DotServiceIntegrationService', () => {
    beforeEach(() => {
        this.injector = DOTTestBed.configureTestingModule({
            providers: [
                DotHttpErrorManagerService,
                DotServiceIntegrationService,
                {
                    provide: LoginService,
                    useClass: LoginServiceMock
                }
            ]
        });

        this.dotServiceIntegrationService = this.injector.get(DotServiceIntegrationService);
        this.dotHttpErrorManagerService = this.injector.get(DotHttpErrorManagerService);
        this.coreWebService = this.injector.get(CoreWebService);
        this.backend = this.injector.get(ConnectionBackend) as MockBackend;
        this.backend.connections.subscribe((connection: any) => {
            this.lastConnection = connection;
        });
    });

    it('should get service integrations', () => {
        const url = 'v1/service-integrations';

        this.dotServiceIntegrationService.get().subscribe((services: DotServiceIntegration[]) => {
            expect(services).toEqual(mockDotServiceIntegration);
        });

        this.lastConnection.mockRespond(
            new Response(
                new ResponseOptions({
                    body: {
                        entity: mockDotServiceIntegration
                    }
                })
            )
        );

        expect(this.lastConnection.request.method).toBe(RequestMethod.Get);
        expect(this.lastConnection.request.url).toContain(url);
    });

    it('should throw error on get service integrations and handle it', () => {
        const error404 = mockResponseView(400);
        spyOn(this.dotHttpErrorManagerService, 'handle').and.callThrough();
        spyOn(this.coreWebService, 'requestView').and.returnValue(throwError(error404));

        this.dotServiceIntegrationService.get().subscribe();
        expect(this.dotHttpErrorManagerService.handle).toHaveBeenCalledWith(mockResponseView(400));
    });

    it('should get a specific service integrations', () => {
        const serviceKey = '1';
        const url = `v1/service-integrations/${serviceKey}`;
        spyOn(this.coreWebService, 'requestView').and.returnValue(
            of({
                entity: mockDotServiceIntegration[0]
            })
        );

        this.dotServiceIntegrationService
            .getConfiguration(serviceKey)
            .subscribe((service: DotServiceIntegration) => {
                expect(service).toEqual(mockDotServiceIntegration[0]);
            });

        expect(this.coreWebService.requestView).toHaveBeenCalledWith({
            method: RequestMethod.Get,
            url: url
        });
    });

    it('should throw error on get a specific service integrations and handle it', () => {
        const error404 = mockResponseView(400);
        spyOn(this.dotHttpErrorManagerService, 'handle').and.callThrough();
        spyOn(this.coreWebService, 'requestView').and.returnValue(throwError(error404));

        this.dotServiceIntegrationService.getConfiguration('test').subscribe();
        expect(this.dotHttpErrorManagerService.handle).toHaveBeenCalledWith(mockResponseView(400));
    });

    it('should delete a specific configuration from a service', () => {
        const serviceKey = '1';
        const hostId = 'abc';
        const url = `v1/service-integrations/${serviceKey}/${hostId}`;

        this.dotServiceIntegrationService
            .deleteConfiguration(serviceKey, hostId)
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

        expect(this.lastConnection.request.method).toBe(RequestMethod.Delete);
        expect(this.lastConnection.request.url).toContain(url);
    });

    it('should throw error on delete a specific service integrations and handle it', () => {
        const error404 = mockResponseView(400);
        spyOn(this.dotHttpErrorManagerService, 'handle').and.callThrough();
        spyOn(this.coreWebService, 'requestView').and.returnValue(throwError(error404));

        this.dotServiceIntegrationService.deleteConfiguration('test', '123').subscribe();
        expect(this.dotHttpErrorManagerService.handle).toHaveBeenCalledWith(mockResponseView(400));
    });

    it('should delete all configurations from a service', () => {
        const serviceKey = '1';
        const url = `v1/service-integrations/${serviceKey}`;

        this.dotServiceIntegrationService
            .deleteAllConfigurations(serviceKey)
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

        expect(this.lastConnection.request.method).toBe(RequestMethod.Delete);
        expect(this.lastConnection.request.url).toContain(url);
    });

    it('should throw error on delete all configurations from a service and handle it', () => {
        const error404 = mockResponseView(400);
        spyOn(this.dotHttpErrorManagerService, 'handle').and.callThrough();
        spyOn(this.coreWebService, 'requestView').and.returnValue(throwError(error404));

        this.dotServiceIntegrationService.deleteAllConfigurations('test').subscribe();
        expect(this.dotHttpErrorManagerService.handle).toHaveBeenCalledWith(mockResponseView(400));
    });
});
