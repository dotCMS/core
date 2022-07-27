/* eslint-disable @typescript-eslint/no-explicit-any */

import { DotAppsService } from './dot-apps.service';
import {
    DotApps,
    DotAppsImportConfiguration,
    DotAppsSaveData
} from '@shared/models/dot-apps/dot-apps.model';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';
import { CoreWebService, LoginService } from '@dotcms/dotcms-js';
import { LoginServiceMock } from '@tests/login-service.mock';
import { TestBed, getTestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpTestingController, HttpClientTestingModule } from '@angular/common/http/testing';
import { CoreWebServiceMock } from '@tests/core-web.service.mock';
import { DotAlertConfirmService } from '@services/dot-alert-confirm';
import { ConfirmationService } from 'primeng/api';
import { DotFormatDateService } from '@services/dot-format-date-service';
import { MockDotRouterService } from '@tests/dot-router-service.mock';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { mockResponseView } from '@tests/response-view.mock';
import { throwError } from 'rxjs';
import * as dotUtils from '@shared/dot-utils';
import { DotFormatDateServiceMock } from '@dotcms/app/test/format-date-service.mock';

const mockDotApps = [
    {
        allowExtraParams: true,
        configurationsCount: 0,
        key: 'google-calendar',
        name: 'Google Calendar',
        description: 'It is a tool to keep track of your events',
        iconUrl: '/dA/d948d85c-3bc8-4d85-b0aa-0e989b9ae235/photo/surfer-profile.jpg'
    },
    {
        allowExtraParams: true,
        configurationsCount: 1,
        key: 'asana',
        name: 'Asana',
        description: 'It is asana to keep track of your asana events',
        iconUrl: '/dA/792c7c9f-6b6f-427b-80ff-1643376c9999/photo/mountain-persona.jpg'
    }
];

describe('DotAppsService', () => {
    let injector: TestBed;
    let dotAppsService: DotAppsService;
    let dotHttpErrorManagerService: DotHttpErrorManagerService;
    let coreWebService: CoreWebService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                {
                    provide: LoginService,
                    useClass: LoginServiceMock
                },
                { provide: DotRouterService, useClass: MockDotRouterService },
                { provide: DotFormatDateService, useClass: DotFormatDateServiceMock },
                ConfirmationService,
                DotAlertConfirmService,
                DotAppsService,
                DotHttpErrorManagerService
            ]
        });
        injector = getTestBed();
        dotAppsService = injector.get(DotAppsService);
        dotHttpErrorManagerService = injector.get(DotHttpErrorManagerService);
        coreWebService = injector.get(CoreWebService);
        httpMock = injector.get(HttpTestingController);
    });

    it('should get apps', () => {
        const url = 'v1/apps';

        dotAppsService.get().subscribe((apps: DotApps[]) => {
            expect(apps).toEqual(mockDotApps);
        });

        const req = httpMock.expectOne(url);
        expect(req.request.method).toBe('GET');
        req.flush({
            entity: mockDotApps
        });
    });

    it('should get filtered app', () => {
        const filter = 'asana';
        const url = `v1/apps?filter=${filter}`;

        dotAppsService.get(filter).subscribe((apps: DotApps[]) => {
            expect(apps).toEqual([mockDotApps[1]]);
        });

        const req = httpMock.expectOne(url);
        expect(req.request.method).toBe('GET');
        req.flush({
            entity: [mockDotApps[1]]
        });
    });

    it('should throw error on get apps and handle it', () => {
        const error404 = mockResponseView(400);
        spyOn(dotHttpErrorManagerService, 'handle').and.callThrough();
        spyOn(coreWebService, 'requestView').and.returnValue(throwError(error404));

        dotAppsService.get().subscribe();
        expect(dotHttpErrorManagerService.handle).toHaveBeenCalledWith(mockResponseView(400));
    });

    it('should get a specific app', () => {
        const appKey = '1';
        const url = `v1/apps/${appKey}`;

        dotAppsService.getConfigurationList(appKey).subscribe((apps: DotApps) => {
            expect(apps).toEqual(mockDotApps[1]);
        });

        const req = httpMock.expectOne(url);
        expect(req.request.method).toBe('GET');
        req.flush({
            entity: mockDotApps[1]
        });
    });

    it('should throw error on get a specific app and handle it', () => {
        const error404 = mockResponseView(400);
        spyOn(dotHttpErrorManagerService, 'handle').and.callThrough();
        spyOn(coreWebService, 'requestView').and.returnValue(throwError(error404));

        dotAppsService.getConfiguration('test', '1').subscribe();
        expect(dotHttpErrorManagerService.handle).toHaveBeenCalledWith(mockResponseView(400));
    });

    it('should import apps', () => {
        spyOn(coreWebService, 'requestView').and.callThrough();
        const conf: DotAppsImportConfiguration = {
            file: null,
            json: { password: 'test' }
        };
        const sentBody = new FormData();
        sentBody.append('json', JSON.stringify(conf.json));
        sentBody.append('file', conf.file);

        dotAppsService.importConfiguration(conf).subscribe((status: string) => {
            expect(status).toEqual('OK');
        });

        const req = httpMock.expectOne(`/api/v1/apps/import`);
        expect(coreWebService.requestView).toHaveBeenCalledWith({
            url: `/api/v1/apps/import`,
            body: sentBody,
            headers: { 'Content-Type': 'multipart/form-data' },
            method: 'POST'
        });

        req.flush({
            entity: 'OK'
        });
    });

    it('should export apps configuration', fakeAsync(() => {
        const blobMock = new Blob(['']);
        const fileName = 'asd-01EDSTVT6KGQ8CQ80PPA8717AN.tar.gz';
        const mockResponse = {
            headers: {
                get: (_header: string) => {
                    return `attachment; filename=${fileName}`;
                }
            },
            blob: () => {
                return blobMock;
            }
        };
        const anchor: HTMLAnchorElement = document.createElement('a');
        spyOn<any>(window, 'fetch').and.returnValue(Promise.resolve(mockResponse));
        spyOn(anchor, 'click');
        spyOn(dotUtils, 'getDownloadLink').and.returnValue(anchor);

        const conf = {
            appKeysBySite: {},
            exportAll: true,
            password: 'test'
        };

        dotAppsService.exportConfiguration(conf);
        tick(1);

        expect(window.fetch).toHaveBeenCalledWith(`/api/v1/apps/export`, {
            method: 'POST',
            cache: 'no-cache',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(conf)
        });
        expect(dotUtils.getDownloadLink).toHaveBeenCalledWith(blobMock, fileName);
        expect(anchor.click).toHaveBeenCalledTimes(1);
    }));

    it('should throw error when export apps configuration', fakeAsync(() => {
        spyOn<any>(window, 'fetch').and.returnValue(Promise.reject(new Error('error')));

        const conf = {
            appKeysBySite: {},
            exportAll: true,
            password: 'test'
        };

        dotAppsService.exportConfiguration(conf).then((error: any) => {
            expect(error).toEqual('error');
        });
        tick(1);
    }));

    it('should save a specific configuration from an app', () => {
        const appKey = '1';
        const hostId = 'abc';
        const params: DotAppsSaveData = {
            name: { hidden: false, value: 'test' }
        };
        const url = `v1/apps/${appKey}/${hostId}`;

        dotAppsService
            .saveSiteConfiguration(appKey, hostId, params)
            .subscribe((response: string) => {
                expect(response).toEqual('ok');
            });

        const req = httpMock.expectOne(url);
        expect(req.request.method).toBe('POST');
        expect(req.request.body).toEqual(params);
        req.flush({
            entity: 'ok'
        });
    });

    it('should throw error on Save a specific app and handle it', () => {
        const params: DotAppsSaveData = {
            name: { hidden: false, value: 'test' }
        };
        const error404 = mockResponseView(400);
        spyOn(dotHttpErrorManagerService, 'handle').and.callThrough();
        spyOn(coreWebService, 'requestView').and.returnValue(throwError(error404));

        dotAppsService.saveSiteConfiguration('test', '123', params).subscribe();
        expect(dotHttpErrorManagerService.handle).toHaveBeenCalledWith(mockResponseView(400));
    });

    it('should delete a specific configuration from an app', () => {
        const appKey = '1';
        const hostId = 'abc';
        const url = `v1/apps/${appKey}/${hostId}`;

        dotAppsService.deleteConfiguration(appKey, hostId).subscribe((response: string) => {
            expect(response).toEqual('ok');
        });

        const req = httpMock.expectOne(url);
        expect(req.request.method).toBe('DELETE');
        req.flush({
            entity: 'ok'
        });
    });

    it('should throw error on delete a specific app and handle it', () => {
        const error404 = mockResponseView(400);
        spyOn(dotHttpErrorManagerService, 'handle').and.callThrough();
        spyOn(coreWebService, 'requestView').and.returnValue(throwError(error404));

        dotAppsService.deleteConfiguration('test', '123').subscribe();
        expect(dotHttpErrorManagerService.handle).toHaveBeenCalledWith(mockResponseView(400));
    });

    it('should delete all configurations from an app', () => {
        const appKey = '1';
        const url = `v1/apps/${appKey}`;

        dotAppsService.deleteAllConfigurations(appKey).subscribe((response: string) => {
            expect(response).toEqual('ok');
        });

        const req = httpMock.expectOne(url);
        expect(req.request.method).toBe('DELETE');
        req.flush({
            entity: 'ok'
        });
    });

    it('should throw error on delete all configurations from an app and handle it', () => {
        const error404 = mockResponseView(400);
        spyOn(dotHttpErrorManagerService, 'handle').and.callThrough();
        spyOn(coreWebService, 'requestView').and.returnValue(throwError(error404));

        dotAppsService.deleteAllConfigurations('test').subscribe();
        expect(dotHttpErrorManagerService.handle).toHaveBeenCalledWith(mockResponseView(400));
    });

    afterEach(() => {
        httpMock.verify();
    });
});
