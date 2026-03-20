import { createServiceFactory, mockProvider, SpectatorService } from '@ngneat/spectator/jest';
import { Subject, of, throwError } from 'rxjs';

import { fakeAsync, tick } from '@angular/core/testing';

import {
    BundleMap,
    DotHttpErrorManagerService,
    DotMessageDisplayService,
    DotMessageService,
    DotOsgiService
} from '@dotcms/data-access';
import { DotcmsEventsService } from '@dotcms/dotcms-js';
import { DotCMSAPIResponse } from '@dotcms/dotcms-models';

import { DotPluginsListStore } from './dot-plugins-list.store';

const MOCK_DOTCMS_API_FIELDS: Pick<
    DotCMSAPIResponse<unknown>,
    'errors' | 'messages' | 'permissions' | 'i18nMessagesMap'
> = {
    errors: [],
    messages: [],
    permissions: [],
    i18nMessagesMap: {}
};

function mockDotCMSResponse<T>(entity: T): DotCMSAPIResponse<T> {
    return { ...MOCK_DOTCMS_API_FIELDS, entity };
}

const MOCK_BUNDLES = [
    {
        bundleId: 1,
        symbolicName: 'test-bundle',
        jarFile: 'test.jar',
        state: 32,
        isSystem: false
    }
];

/**
 * Spectator's mockProvider reuses the same jest.fn() instances across tests.
 * Tests that call jest.spyOn(...).mockReturnValue(...) otherwise leak those
 * implementations into later examples (breaking forkJoin / optional callbacks).
 */
function resetOsgiServiceMocks(osgi: DotOsgiService): void {
    jest.mocked(osgi.getInstalledBundles).mockReset();
    jest.mocked(osgi.getInstalledBundles).mockReturnValue(
        of(mockDotCMSResponse(MOCK_BUNDLES as BundleMap[]))
    );
    jest.mocked(osgi.getAvailablePlugins).mockReset();
    jest.mocked(osgi.getAvailablePlugins).mockReturnValue(of(mockDotCMSResponse(['a.jar'])));
    jest.mocked(osgi.uploadBundles).mockReset();
    jest.mocked(osgi.uploadBundles).mockReturnValue(of(mockDotCMSResponse({})));
    jest.mocked(osgi.deploy).mockReset();
    jest.mocked(osgi.deploy).mockReturnValue(of(mockDotCMSResponse({})));
    jest.mocked(osgi.start).mockReset();
    jest.mocked(osgi.start).mockReturnValue(of(mockDotCMSResponse({})));
    jest.mocked(osgi.stop).mockReset();
    jest.mocked(osgi.stop).mockReturnValue(of(mockDotCMSResponse({})));
    jest.mocked(osgi.undeploy).mockReset();
    jest.mocked(osgi.undeploy).mockReturnValue(of(mockDotCMSResponse({})));
    jest.mocked(osgi.processExports).mockReset();
    jest.mocked(osgi.processExports).mockReturnValue(of(mockDotCMSResponse({})));
    jest.mocked(osgi.restart).mockReset();
    jest.mocked(osgi.restart).mockReturnValue(of(mockDotCMSResponse({})));
}

describe('DotPluginsListStore', () => {
    let spectator: SpectatorService<InstanceType<typeof DotPluginsListStore>>;
    let store: InstanceType<typeof DotPluginsListStore>;
    let osgiService: DotOsgiService;
    let httpErrorManager: DotHttpErrorManagerService;

    const osgiFrameworkRestartSubject = new Subject<void>();
    const osgiBundlesLoadedSubject = new Subject<void>();

    const createService = createServiceFactory({
        service: DotPluginsListStore,
        providers: [
            mockProvider(DotOsgiService, {
                getInstalledBundles: jest
                    .fn()
                    .mockReturnValue(of(mockDotCMSResponse(MOCK_BUNDLES as BundleMap[]))),
                getAvailablePlugins: jest.fn().mockReturnValue(of(mockDotCMSResponse(['a.jar']))),
                uploadBundles: jest.fn().mockReturnValue(of(mockDotCMSResponse({}))),
                deploy: jest.fn().mockReturnValue(of(mockDotCMSResponse({}))),
                start: jest.fn().mockReturnValue(of(mockDotCMSResponse({}))),
                stop: jest.fn().mockReturnValue(of(mockDotCMSResponse({}))),
                undeploy: jest.fn().mockReturnValue(of(mockDotCMSResponse({}))),
                processExports: jest.fn().mockReturnValue(of(mockDotCMSResponse({}))),
                restart: jest.fn().mockReturnValue(of(mockDotCMSResponse({})))
            }),
            mockProvider(DotHttpErrorManagerService, { handle: jest.fn() }),
            mockProvider(DotMessageDisplayService, { push: jest.fn() }),
            mockProvider(DotMessageService, { get: (key: string) => key }),
            mockProvider(DotcmsEventsService, {
                subscribeTo: jest.fn().mockImplementation((event: string) => {
                    if (event === 'OSGI_FRAMEWORK_RESTART')
                        return osgiFrameworkRestartSubject.asObservable();
                    if (event === 'OSGI_BUNDLES_LOADED')
                        return osgiBundlesLoadedSubject.asObservable();
                    return of();
                })
            })
        ]
    });

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;
        osgiService = spectator.inject(DotOsgiService);
        httpErrorManager = spectator.inject(DotHttpErrorManagerService);
        jest.mocked(httpErrorManager.handle).mockClear();
        spectator.flushEffects();
    });

    afterEach(() => {
        resetOsgiServiceMocks(osgiService);
    });

    describe('init', () => {
        it('should load bundles and available plugins on init', () => {
            expect(store.bundles()).toEqual(MOCK_BUNDLES);
            expect(store.availableJars()).toEqual(['a.jar']);
            expect(store.status()).toBe('loaded');
        });

        it('should always request bundles with system bundles ignored', () => {
            expect(osgiService.getInstalledBundles).toHaveBeenCalledWith(true);
        });
    });

    describe('rows()', () => {
        it('should merge installed bundles and available jars into a single list', () => {
            expect(store.rows()).toHaveLength(2);
        });

        it('should map installed bundle fields to row', () => {
            const row = store.rows()[0];
            expect(row.jarFile).toBe('test.jar');
            expect(row.symbolicName).toBe('test-bundle');
            expect(row.state).toBe(32);
            expect(row.bundleId).toBe(1);
        });

        it('should map available jars as undeployed rows', () => {
            const row = store.rows()[1];
            expect(row.jarFile).toBe('a.jar');
            expect(row.symbolicName).toBe('a.jar');
            expect(row.state).toBe('undeployed');
        });

        it('should fall back symbolicName to jarFile when symbolicName is empty', () => {
            jest.spyOn(osgiService, 'getInstalledBundles').mockReturnValue(
                of(mockDotCMSResponse([{ ...(MOCK_BUNDLES[0] as BundleMap), symbolicName: '' }]))
            );
            store.loadBundles();
            expect(store.rows()[0].symbolicName).toBe('test.jar');
        });
    });

    describe('loadBundles error', () => {
        it('should handle HTTP error and set status to error', () => {
            const error = new Error('HTTP error');
            jest.spyOn(osgiService, 'getInstalledBundles').mockReturnValue(throwError(error));
            store.loadBundles();
            expect(httpErrorManager.handle).toHaveBeenCalledWith(error);
            expect(store.status()).toBe('error');
        });
    });

    describe('loadAvailablePlugins error', () => {
        it('should handle HTTP error', () => {
            const error = new Error('HTTP error');
            jest.spyOn(osgiService, 'getAvailablePlugins').mockReturnValue(throwError(error));
            store.loadAvailablePlugins();
            expect(httpErrorManager.handle).toHaveBeenCalledWith(error);
        });
    });

    describe('uploadBundles', () => {
        it('should upload files and reload bundles and available jars on success', fakeAsync(() => {
            const files = [new File(['content'], 'plugin.jar')];
            const installedCallsBefore = (osgiService.getInstalledBundles as jest.Mock).mock.calls
                .length;
            const pluginsCallsBefore = (osgiService.getAvailablePlugins as jest.Mock).mock.calls
                .length;

            store.uploadBundles(files);
            tick();

            expect(osgiService.uploadBundles).toHaveBeenCalledWith(files);
            expect(
                (osgiService.getInstalledBundles as jest.Mock).mock.calls.length
            ).toBeGreaterThan(installedCallsBefore);
            expect(
                (osgiService.getAvailablePlugins as jest.Mock).mock.calls.length
            ).toBeGreaterThan(pluginsCallsBefore);
        }));

        it('should invoke optional callback on success', fakeAsync(() => {
            const callback = jest.fn();
            store.uploadBundles([new File(['content'], 'plugin.jar')], callback);
            tick();
            expect(callback).toHaveBeenCalled();
        }));

        it('should handle upload error', () => {
            const error = new Error('Upload failed');
            jest.spyOn(osgiService, 'uploadBundles').mockReturnValue(throwError(error));
            store.uploadBundles([new File(['content'], 'plugin.jar')]);
            expect(httpErrorManager.handle).toHaveBeenCalledWith(error);
        });
    });

    describe('deploy', () => {
        it('should call deploy and reload bundles and available jars on success', () => {
            store.deploy('plugin.jar');
            expect(osgiService.deploy).toHaveBeenCalledWith('plugin.jar');
            expect(osgiService.getInstalledBundles).toHaveBeenCalled();
            expect(osgiService.getAvailablePlugins).toHaveBeenCalled();
        });

        it('should keep status as loaded on error so the table stays usable', () => {
            jest.spyOn(osgiService, 'deploy').mockReturnValue(throwError(new Error()));
            store.deploy('plugin.jar');
            expect(store.status()).toBe('loaded');
        });

        it('should handle deploy error', () => {
            const error = new Error('Deploy failed');
            jest.spyOn(osgiService, 'deploy').mockReturnValue(throwError(error));
            store.deploy('plugin.jar');
            expect(httpErrorManager.handle).toHaveBeenCalledWith(error);
        });
    });

    describe('start', () => {
        it('should call start and reload bundles on success', () => {
            store.start('test.jar');
            expect(osgiService.start).toHaveBeenCalledWith('test.jar');
            expect(osgiService.getInstalledBundles).toHaveBeenCalled();
        });

        it('should handle start error', () => {
            const error = new Error('Start failed');
            jest.spyOn(osgiService, 'start').mockReturnValue(throwError(error));
            store.start('test.jar');
            expect(httpErrorManager.handle).toHaveBeenCalledWith(error);
        });
    });

    describe('stop', () => {
        it('should call stop and reload bundles on success', () => {
            store.stop('test.jar');
            expect(osgiService.stop).toHaveBeenCalledWith('test.jar');
            expect(osgiService.getInstalledBundles).toHaveBeenCalled();
        });

        it('should handle stop error', () => {
            const error = new Error('Stop failed');
            jest.spyOn(osgiService, 'stop').mockReturnValue(throwError(error));
            store.stop('test.jar');
            expect(httpErrorManager.handle).toHaveBeenCalledWith(error);
        });
    });

    describe('undeploy', () => {
        it('should call undeploy and reload bundles and available jars on success', () => {
            store.undeploy('test.jar');
            expect(osgiService.undeploy).toHaveBeenCalledWith('test.jar');
            expect(osgiService.getInstalledBundles).toHaveBeenCalled();
            expect(osgiService.getAvailablePlugins).toHaveBeenCalled();
        });

        it('should handle undeploy error', () => {
            const error = new Error('Undeploy failed');
            jest.spyOn(osgiService, 'undeploy').mockReturnValue(throwError(error));
            store.undeploy('test.jar');
            expect(httpErrorManager.handle).toHaveBeenCalledWith(error);
        });
    });

    describe('processExports', () => {
        it('should call processExports with the bundle symbolic name and reload bundles', () => {
            store.processExports('test-bundle');
            expect(osgiService.processExports).toHaveBeenCalledWith('test-bundle');
            expect(osgiService.getInstalledBundles).toHaveBeenCalled();
        });

        it('should handle processExports error', () => {
            const error = new Error('ProcessExports failed');
            jest.spyOn(osgiService, 'processExports').mockReturnValue(throwError(error));
            store.processExports('test-bundle');
            expect(httpErrorManager.handle).toHaveBeenCalledWith(error);
        });
    });

    describe('restart', () => {
        it('should set status to restarting immediately, then reload after delay', fakeAsync(() => {
            const callback = jest.fn();
            store.restart(callback);

            expect(osgiService.restart).toHaveBeenCalled();
            expect(store.status()).toBe('restarting');
            expect(callback).not.toHaveBeenCalled();

            const installedCallsBefore = (osgiService.getInstalledBundles as jest.Mock).mock.calls
                .length;

            tick(5000);

            expect(
                (osgiService.getInstalledBundles as jest.Mock).mock.calls.length
            ).toBeGreaterThan(installedCallsBefore);
            expect(osgiService.getAvailablePlugins).toHaveBeenCalled();
            expect(callback).toHaveBeenCalled();
        }));

        it('should handle restart error', () => {
            const error = new Error('Restart failed');
            jest.spyOn(osgiService, 'restart').mockReturnValue(throwError(error));
            store.restart();
            expect(httpErrorManager.handle).toHaveBeenCalledWith(error);
        });
    });

    describe('WebSocket events', () => {
        beforeEach(() => {
            jest.useFakeTimers();
        });

        afterEach(() => {
            jest.useRealTimers();
        });

        it('should set status to restarting on OSGI_FRAMEWORK_RESTART', () => {
            osgiFrameworkRestartSubject.next();
            expect(store.status()).toBe('restarting');
        });

        it('should reload bundles after OSGI_BUNDLES_LOADED with 5s debounce', () => {
            const getInstalledBundlesSpy = jest
                .spyOn(osgiService, 'getInstalledBundles')
                .mockReturnValue(of(mockDotCMSResponse([] as BundleMap[])));
            const getAvailablePluginsSpy = jest
                .spyOn(osgiService, 'getAvailablePlugins')
                .mockReturnValue(of(mockDotCMSResponse([] as string[])));

            osgiBundlesLoadedSubject.next();
            jest.advanceTimersByTime(5000);

            expect(getInstalledBundlesSpy).toHaveBeenCalled();
            expect(getAvailablePluginsSpy).toHaveBeenCalled();
        });

        it('should not reload before 5s debounce elapses', () => {
            const initialCalls = (osgiService.getInstalledBundles as jest.Mock).mock.calls.length;

            osgiBundlesLoadedSubject.next();
            jest.advanceTimersByTime(4999);

            expect((osgiService.getInstalledBundles as jest.Mock).mock.calls.length).toBe(
                initialCalls
            );
        });
    });
});
