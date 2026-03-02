/* eslint-disable @typescript-eslint/no-explicit-any */
import { createServiceFactory, mockProvider, SpectatorService } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { fakeAsync, tick } from '@angular/core/testing';

import { DotApp, DotAppsImportConfiguration, DotAppsSaveData } from '@dotcms/dotcms-models';
import * as dotUtils from '@dotcms/utils/lib/dot-utils';

import { DotAppsService } from './dot-apps.service';

import { DotHttpErrorManagerService } from '../dot-http-error-manager/dot-http-error-manager.service';

const mockDotApps: DotApp[] = [
    {
        allowExtraParams: true,
        configurationsCount: 0,
        key: 'google-calendar',
        name: 'Google Calendar',
        description: 'It is a tool to keep track of your events',
        iconUrl: '/dA/d948d85c-3bc8-4d85-b0aa-0e989b9ae235/photo/surfer-profile.jpg'
    } as DotApp,
    {
        allowExtraParams: true,
        configurationsCount: 1,
        key: 'asana',
        name: 'Asana',
        description: 'It is asana to keep track of your asana events',
        iconUrl: '/dA/792c7c9f-6b6f-427b-80ff-1643376c9999/photo/mountain-persona.jpg'
    } as DotApp
];

describe('DotAppsService', () => {
    let spectator: SpectatorService<DotAppsService>;
    let httpMock: HttpTestingController;

    const createService = createServiceFactory({
        service: DotAppsService,
        providers: [
            provideHttpClient(),
            provideHttpClientTesting(),
            mockProvider(DotHttpErrorManagerService)
        ]
    });

    beforeEach(() => {
        spectator = createService();
        httpMock = spectator.inject(HttpTestingController);
    });

    afterEach(() => {
        httpMock.verify();
    });

    describe('get', () => {
        it('should get apps', () => {
            spectator.service.get().subscribe((apps) => {
                expect(apps).toEqual(mockDotApps);
            });

            const req = httpMock.expectOne('/api/v1/apps');
            expect(req.request.method).toBe('GET');
            req.flush({ entity: mockDotApps });
        });

        it('should get filtered apps', () => {
            const filter = 'asana';

            spectator.service.get(filter).subscribe((apps) => {
                expect(apps).toEqual([mockDotApps[1]]);
            });

            const req = httpMock.expectOne(`/api/v1/apps?filter=${filter}`);
            expect(req.request.method).toBe('GET');
            req.flush({ entity: [mockDotApps[1]] });
        });

        it('should handle error and return null', () => {
            const errorManagerService = spectator.inject(DotHttpErrorManagerService);
            jest.spyOn(errorManagerService, 'handle').mockReturnValue(of({ status: 400 } as any));

            spectator.service.get().subscribe((result) => {
                expect(result).toBeNull();
            });

            const req = httpMock.expectOne('/api/v1/apps');
            req.flush('Error', { status: 400, statusText: 'Bad Request' });

            expect(errorManagerService.handle).toHaveBeenCalled();
        });
    });

    describe('getConfigurationList', () => {
        it('should get configuration list for an app', () => {
            const appKey = 'test-app';

            spectator.service.getConfigurationList(appKey).subscribe((app) => {
                expect(app).toEqual(mockDotApps[1]);
            });

            const req = httpMock.expectOne(`/api/v1/apps/${appKey}`);
            expect(req.request.method).toBe('GET');
            req.flush({ entity: mockDotApps[1] });
        });

        it('should handle error and return null', () => {
            const errorManagerService = spectator.inject(DotHttpErrorManagerService);
            jest.spyOn(errorManagerService, 'handle').mockReturnValue(of({ status: 400 } as any));

            spectator.service.getConfigurationList('test').subscribe((result) => {
                expect(result).toBeNull();
            });

            const req = httpMock.expectOne('/api/v1/apps/test');
            req.flush('Error', { status: 400, statusText: 'Bad Request' });

            expect(errorManagerService.handle).toHaveBeenCalled();
        });
    });

    describe('getConfiguration', () => {
        it('should get a specific app configuration', () => {
            const appKey = 'test';
            const id = '1';

            spectator.service.getConfiguration(appKey, id).subscribe((app) => {
                expect(app).toEqual(mockDotApps[1]);
            });

            const req = httpMock.expectOne(`/api/v1/apps/${appKey}/${id}`);
            expect(req.request.method).toBe('GET');
            req.flush({ entity: mockDotApps[1] });
        });

        it('should handle error and return null', () => {
            const errorManagerService = spectator.inject(DotHttpErrorManagerService);
            jest.spyOn(errorManagerService, 'handle').mockReturnValue(of({ status: 400 } as any));

            spectator.service.getConfiguration('test', '1').subscribe((result) => {
                expect(result).toBeNull();
            });

            const req = httpMock.expectOne('/api/v1/apps/test/1');
            req.flush('Error', { status: 400, statusText: 'Bad Request' });

            expect(errorManagerService.handle).toHaveBeenCalled();
        });
    });

    describe('saveSiteConfiguration', () => {
        it('should save a configuration', () => {
            const appKey = '1';
            const hostId = 'abc';
            const params: DotAppsSaveData = {
                name: { hidden: false, value: 'test' }
            };

            spectator.service
                .saveSiteConfiguration(appKey, hostId, params)
                .subscribe((response) => {
                    expect(response).toEqual('ok');
                });

            const req = httpMock.expectOne(`/api/v1/apps/${appKey}/${hostId}`);
            expect(req.request.method).toBe('POST');
            expect(req.request.body).toEqual(params);
            req.flush({ entity: 'ok' });
        });

        it('should handle error and return null', () => {
            const errorManagerService = spectator.inject(DotHttpErrorManagerService);
            jest.spyOn(errorManagerService, 'handle').mockReturnValue(of({ status: 400 } as any));

            const params: DotAppsSaveData = { name: { hidden: false, value: 'test' } };

            spectator.service.saveSiteConfiguration('test', '123', params).subscribe((result) => {
                expect(result).toBeNull();
            });

            const req = httpMock.expectOne('/api/v1/apps/test/123');
            req.flush('Error', { status: 400, statusText: 'Bad Request' });

            expect(errorManagerService.handle).toHaveBeenCalled();
        });
    });

    describe('importConfiguration', () => {
        it('should import configuration', () => {
            const conf: DotAppsImportConfiguration = {
                file: new File([], 'test.json'),
                json: { password: 'test' }
            };

            spectator.service.importConfiguration(conf).subscribe((status) => {
                expect(status).toEqual('OK');
            });

            const req = httpMock.expectOne('/api/v1/apps/import');
            expect(req.request.method).toBe('POST');
            req.flush({ entity: 'OK' });
        });

        it('should handle error and return status string', () => {
            const errorManagerService = spectator.inject(DotHttpErrorManagerService);
            jest.spyOn(errorManagerService, 'handle').mockReturnValue(of({ status: 400 } as any));

            const conf: DotAppsImportConfiguration = {
                file: new File([], 'test.json'),
                json: { password: 'test' }
            };

            spectator.service.importConfiguration(conf).subscribe((result) => {
                expect(result).toBe('400');
            });

            const req = httpMock.expectOne('/api/v1/apps/import');
            req.flush('Error', { status: 400, statusText: 'Bad Request' });

            expect(errorManagerService.handle).toHaveBeenCalled();
        });
    });

    describe('exportConfiguration', () => {
        it('should export configuration and trigger download', fakeAsync(() => {
            const blobMock = new Blob(['']);
            const fileName = 'apps-export.tar.gz';
            const mockResponse = {
                headers: {
                    get: (header: string) => {
                        if (header === 'content-disposition') {
                            return `attachment; filename=${fileName}`;
                        }

                        return null;
                    }
                },
                blob: () => blobMock
            };

            const anchor = document.createElement('a');
            (window as any).fetch = jest.fn().mockReturnValue(Promise.resolve(mockResponse));
            jest.spyOn(anchor, 'click');
            jest.spyOn(dotUtils, 'getDownloadLink').mockReturnValue(anchor);

            const conf = {
                appKeysBySite: {},
                exportAll: true,
                password: 'test'
            };

            spectator.service.exportConfiguration(conf);
            tick(1);

            expect((window as any).fetch).toHaveBeenCalledWith('/api/v1/apps/export', {
                method: 'POST',
                cache: 'no-cache',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(conf)
            });
            expect(dotUtils.getDownloadLink).toHaveBeenCalledWith(blobMock, fileName);
            expect(anchor.click).toHaveBeenCalledTimes(1);
        }));

        it('should handle export error', fakeAsync(() => {
            (window as any).fetch = jest
                .fn()
                .mockReturnValue(Promise.reject(new Error('export failed')));

            const conf = {
                appKeysBySite: {},
                exportAll: true,
                password: 'test'
            };

            spectator.service.exportConfiguration(conf).then((error) => {
                expect(error).toEqual('export failed');
            });

            tick(1);
        }));
    });

    describe('deleteConfiguration', () => {
        it('should delete a specific configuration', () => {
            const appKey = '1';
            const hostId = 'abc';

            spectator.service.deleteConfiguration(appKey, hostId).subscribe((response) => {
                expect(response).toEqual('ok');
            });

            const req = httpMock.expectOne(`/api/v1/apps/${appKey}/${hostId}`);
            expect(req.request.method).toBe('DELETE');
            req.flush({ entity: 'ok' });
        });

        it('should handle error and return null', () => {
            const errorManagerService = spectator.inject(DotHttpErrorManagerService);
            jest.spyOn(errorManagerService, 'handle').mockReturnValue(of({ status: 400 } as any));

            spectator.service.deleteConfiguration('test', '123').subscribe((result) => {
                expect(result).toBeNull();
            });

            const req = httpMock.expectOne('/api/v1/apps/test/123');
            req.flush('Error', { status: 400, statusText: 'Bad Request' });

            expect(errorManagerService.handle).toHaveBeenCalled();
        });
    });

    describe('deleteAllConfigurations', () => {
        it('should delete all configurations from an app', () => {
            const appKey = '1';

            spectator.service.deleteAllConfigurations(appKey).subscribe((response) => {
                expect(response).toEqual('ok');
            });

            const req = httpMock.expectOne(`/api/v1/apps/${appKey}`);
            expect(req.request.method).toBe('DELETE');
            req.flush({ entity: 'ok' });
        });

        it('should handle error and return null', () => {
            const errorManagerService = spectator.inject(DotHttpErrorManagerService);
            jest.spyOn(errorManagerService, 'handle').mockReturnValue(of({ status: 400 } as any));

            spectator.service.deleteAllConfigurations('test').subscribe((result) => {
                expect(result).toBeNull();
            });

            const req = httpMock.expectOne('/api/v1/apps/test');
            req.flush('Error', { status: 400, statusText: 'Bad Request' });

            expect(errorManagerService.handle).toHaveBeenCalled();
        });
    });
});
