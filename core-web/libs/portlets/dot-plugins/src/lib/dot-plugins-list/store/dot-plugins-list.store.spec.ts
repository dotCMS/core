import { createServiceFactory, mockProvider, SpectatorService } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { DotHttpErrorManagerService, DotOsgiService } from '@dotcms/data-access';

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

    const createService = createServiceFactory({
        service: DotPluginsListStore,
        providers: [
            mockProvider(DotOsgiService, {
                getInstalledBundles: jest.fn().mockReturnValue(of({ entity: MOCK_BUNDLES })),
                getAvailablePlugins: jest.fn().mockReturnValue(of({ entity: ['a.jar'] }))
            }),
            mockProvider(DotHttpErrorManagerService)
        ]
    });

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;
        spectator.flushEffects();
    });

    it('should load bundles and available plugins on init', () => {
        expect(store.bundles()).toEqual(MOCK_BUNDLES);
        expect(store.availableJars()).toEqual(['a.jar']);
        expect(store.status()).toBe('loaded');
    });

    it('should set ignoreSystemBundles', () => {
        store.setIgnoreSystemBundles(false);
        expect(store.ignoreSystemBundles()).toBe(false);
    });
});
