import { createServiceFactory, mockProvider, SpectatorService } from '@ngneat/spectator/jest';
import { Subject, of, throwError } from 'rxjs';

import {
    DotHttpErrorManagerService,
    DotMessageDisplayService,
    DotMessageService,
    DotOsgiService
} from '@dotcms/data-access';
import { DotcmsEventsService } from '@dotcms/dotcms-js';

import { DotPluginsListStore } from './dot-plugins-list.store';

const MOCK_BUNDLES = [
    {
        bundleId: 1,
        symbolicName: 'test-bundle',
        jarFile: 'test.jar',
        state: 32,
        isSystem: false
    }
];

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
                getInstalledBundles: jest.fn().mockReturnValue(of({ entity: MOCK_BUNDLES })),
                getAvailablePlugins: jest.fn().mockReturnValue(of({ entity: ['a.jar'] })),
                uploadBundles: jest.fn().mockReturnValue(of({})),
                deploy: jest.fn().mockReturnValue(of({})),
                start: jest.fn().mockReturnValue(of({})),
                stop: jest.fn().mockReturnValue(of({})),
                undeploy: jest.fn().mockReturnValue(of({})),
                processExports: jest.fn().mockReturnValue(of({})),
                restart: jest.fn().mockReturnValue(of({}))
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
        spectator.flushEffects();
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
                of({ entity: [{ ...MOCK_BUNDLES[0], symbolicName: '' }] })
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
        it('should upload files and reload bundles and available jars on success', () => {
            const files = [new File(['content'], 'plugin.jar')];
            store.uploadBundles(files);
            expect(osgiService.uploadBundles).toHaveBeenCalledWith(files);
            expect(osgiService.getInstalledBundles).toHaveBeenCalled();
            expect(osgiService.getAvailablePlugins).toHaveBeenCalled();
        });

        it('should invoke optional callback on success', () => {
            const callback = jest.fn();
            store.uploadBundles([new File(['content'], 'plugin.jar')], callback);
            expect(callback).toHaveBeenCalled();
        });

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
        it('should reload bundles and available jars after restart', () => {
            const callback = jest.fn();
            store.restart(callback);
            expect(osgiService.restart).toHaveBeenCalled();
            expect(osgiService.getInstalledBundles).toHaveBeenCalled();
            expect(osgiService.getAvailablePlugins).toHaveBeenCalled();
            expect(callback).toHaveBeenCalled();
        });

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
                .mockReturnValue(of({ entity: [] }));
            const getAvailablePluginsSpy = jest
                .spyOn(osgiService, 'getAvailablePlugins')
                .mockReturnValue(of({ entity: [] }));

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
