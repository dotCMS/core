/* eslint-disable @typescript-eslint/no-explicit-any */
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { fakeAsync, TestBed, tick } from '@angular/core/testing';

import { DotHttpErrorManagerService } from '@dotcms/data-access';
import { DotApp, DotAppsImportConfiguration, DotAppsSaveData } from '@dotcms/dotcms-models';
import * as dotUtils from '@dotcms/utils/lib/dot-utils';
import { MockDotHttpErrorManagerService } from '@dotcms/utils-testing';

import { DotAppsService } from './dot-apps.service';

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
    let dotAppsService: DotAppsService;
    let dotHttpErrorManagerService: DotHttpErrorManagerService;
    let httpTesting: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                provideHttpClient(),
                provideHttpClientTesting(),
                DotAppsService,
                {
                    provide: DotHttpErrorManagerService,
                    useClass: MockDotHttpErrorManagerService
                }
            ]
        });

        dotAppsService = TestBed.inject(DotAppsService);
        dotHttpErrorManagerService = TestBed.inject(DotHttpErrorManagerService);
        httpTesting = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
        httpTesting.verify();
    });

    it('should get apps', () => {
        dotAppsService.get().subscribe((apps: DotApp[]) => {
            expect(apps).toEqual(mockDotApps);
        });

        const req = httpTesting.expectOne('/api/v1/apps');
        expect(req.request.method).toBe('GET');
        req.flush({
            entity: mockDotApps
        });
    });

    it('should get filtered app', () => {
        const filter = 'asana';

        dotAppsService.get(filter).subscribe((apps: DotApp[]) => {
            expect(apps).toEqual([mockDotApps[1]]);
        });

        const req = httpTesting.expectOne(`/api/v1/apps?filter=${filter}`);
        expect(req.request.method).toBe('GET');
        req.flush({
            entity: [mockDotApps[1]]
        });
    });

    it('should throw error on get apps and handle it', () => {
        jest.spyOn(dotHttpErrorManagerService, 'handle');

        dotAppsService.get().subscribe();

        const req = httpTesting.expectOne('/api/v1/apps');
        req.flush(null, { status: 400, statusText: 'Bad Request' });

        expect(dotHttpErrorManagerService.handle).toHaveBeenCalled();
    });

    it('should get a specific app', () => {
        const appKey = '1';

        dotAppsService.getConfigurationList(appKey).subscribe((apps: DotApp) => {
            expect(apps).toEqual(mockDotApps[1]);
        });

        const req = httpTesting.expectOne(`/api/v1/apps/${appKey}`);
        expect(req.request.method).toBe('GET');
        req.flush({
            entity: mockDotApps[1]
        });
    });

    it('should get a specific configuration of an app', () => {
        const appKey = 'test';
        const id = '1';

        dotAppsService.getConfiguration(appKey, id).subscribe((app: DotApp) => {
            expect(app).toEqual(mockDotApps[0]);
        });

        const req = httpTesting.expectOne(`/api/v1/apps/${appKey}/${id}`);
        expect(req.request.method).toBe('GET');
        req.flush({
            entity: mockDotApps[0]
        });
    });

    it('should throw error on get a specific app and handle it', () => {
        jest.spyOn(dotHttpErrorManagerService, 'handle');

        dotAppsService.getConfiguration('test', '1').subscribe();

        const req = httpTesting.expectOne('/api/v1/apps/test/1');
        req.flush(null, { status: 400, statusText: 'Bad Request' });

        expect(dotHttpErrorManagerService.handle).toHaveBeenCalled();
    });

    it('should import apps', () => {
        const conf: DotAppsImportConfiguration = {
            file: null,
            json: { password: 'test' }
        };

        dotAppsService.importConfiguration(conf).subscribe((status: string) => {
            expect(status).toEqual('OK');
        });

        const req = httpTesting.expectOne('/api/v1/apps/import');
        expect(req.request.method).toBe('POST');
        expect(req.request.body instanceof FormData).toBeTruthy();
        req.flush({
            entity: 'OK'
        });
    });

    it('should throw error on import apps and handle it', () => {
        jest.spyOn(dotHttpErrorManagerService, 'handle');

        const conf: DotAppsImportConfiguration = {
            file: null,
            json: { password: 'test' }
        };

        dotAppsService.importConfiguration(conf).subscribe();

        const req = httpTesting.expectOne('/api/v1/apps/import');
        req.flush(null, { status: 400, statusText: 'Bad Request' });

        expect(dotHttpErrorManagerService.handle).toHaveBeenCalled();
    });

    it('should export apps configuration', fakeAsync(() => {
        const blobMock = new Blob(['']);
        const fileName = 'asd-01EDSTVT6KGQ8CQ80PPA8717AN.tar.gz';
        const mockResponse = {
            headers: {
                get: (_header: string) => {
                    if (_header === 'content-disposition') {
                        return `attachment; filename=${fileName}`;
                    }

                    if (_header === 'error-message') {
                        return null;
                    }

                    return null;
                }
            },
            blob: () => {
                return blobMock;
            }
        };
        const anchor: HTMLAnchorElement = document.createElement('a');
        (window as any).fetch = jest.fn().mockReturnValue(Promise.resolve(mockResponse));
        jest.spyOn(anchor, 'click');
        jest.spyOn(dotUtils, 'getDownloadLink').mockReturnValue(anchor);

        const conf = {
            appKeysBySite: {},
            exportAll: true,
            password: 'test'
        };

        dotAppsService.exportConfiguration(conf);
        tick(1);

        expect((window as any).fetch).toHaveBeenCalledWith(`/api/v1/apps/export`, {
            method: 'POST',
            cache: 'no-cache',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(conf)
        });
        expect(dotUtils.getDownloadLink).toHaveBeenCalledWith(blobMock, fileName);
        expect(dotUtils.getDownloadLink).toHaveBeenCalledTimes(1);
        expect(anchor.click).toHaveBeenCalledTimes(1);
    }));

    it('should throw error when export apps configuration', fakeAsync(() => {
        (window as any).fetch = jest.fn().mockReturnValue(Promise.reject(new Error('error')));

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

        dotAppsService
            .saveSiteConfiguration(appKey, hostId, params)
            .subscribe((response: string) => {
                expect(response).toEqual('ok');
            });

        const req = httpTesting.expectOne(`/api/v1/apps/${appKey}/${hostId}`);
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
        jest.spyOn(dotHttpErrorManagerService, 'handle');

        dotAppsService.saveSiteConfiguration('test', '123', params).subscribe();

        const req = httpTesting.expectOne('/api/v1/apps/test/123');
        req.flush(null, { status: 400, statusText: 'Bad Request' });

        expect(dotHttpErrorManagerService.handle).toHaveBeenCalled();
    });

    it('should delete a specific configuration from an app', () => {
        const appKey = '1';
        const hostId = 'abc';

        dotAppsService.deleteConfiguration(appKey, hostId).subscribe((response: string) => {
            expect(response).toEqual('ok');
        });

        const req = httpTesting.expectOne(`/api/v1/apps/${appKey}/${hostId}`);
        expect(req.request.method).toBe('DELETE');
        req.flush({
            entity: 'ok'
        });
    });

    it('should throw error on delete a specific app and handle it', () => {
        jest.spyOn(dotHttpErrorManagerService, 'handle');

        dotAppsService.deleteConfiguration('test', '123').subscribe();

        const req = httpTesting.expectOne('/api/v1/apps/test/123');
        req.flush(null, { status: 400, statusText: 'Bad Request' });

        expect(dotHttpErrorManagerService.handle).toHaveBeenCalled();
    });

    it('should delete all configurations from an app', () => {
        const appKey = '1';

        dotAppsService.deleteAllConfigurations(appKey).subscribe((response: string) => {
            expect(response).toEqual('ok');
        });

        const req = httpTesting.expectOne(`/api/v1/apps/${appKey}`);
        expect(req.request.method).toBe('DELETE');
        req.flush({
            entity: 'ok'
        });
    });

    it('should throw error on delete all configurations from an app and handle it', () => {
        jest.spyOn(dotHttpErrorManagerService, 'handle');

        dotAppsService.deleteAllConfigurations('test').subscribe();

        const req = httpTesting.expectOne('/api/v1/apps/test');
        req.flush(null, { status: 400, statusText: 'Bad Request' });

        expect(dotHttpErrorManagerService.handle).toHaveBeenCalled();
    });
});
