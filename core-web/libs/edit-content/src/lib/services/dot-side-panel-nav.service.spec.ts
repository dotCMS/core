import { createServiceFactory, mockProvider, SpectatorService } from '@openng/spectator/jest';

import { GlobalStore } from '@dotcms/store';

import { DotSidePanelNavController } from './dot-side-panel-nav.service';

const LS_KEY = 'dot-edit-content-side-panel-prev-nav-collapsed';

describe('DotSidePanelNavController', () => {
    let spectator: SpectatorService<DotSidePanelNavController>;
    let service: DotSidePanelNavController;
    let globalStore: {
        isNavigationCollapsed: jest.Mock;
        collapseNavigation: jest.Mock;
        expandNavigation: jest.Mock;
    };

    const createService = createServiceFactory({
        service: DotSidePanelNavController,
        providers: [
            mockProvider(GlobalStore, {
                isNavigationCollapsed: jest.fn().mockReturnValue(false),
                collapseNavigation: jest.fn(),
                expandNavigation: jest.fn()
            })
        ]
    });

    const setViewportWidth = (width: number) =>
        Object.defineProperty(window, 'innerWidth', { value: width, configurable: true });

    /** Simulate a wide viewport (>= threshold → no collapse). */
    const setWideViewport = () => setViewportWidth(1920);

    beforeEach(() => {
        localStorage.clear();
        // Narrow viewport by default so the collapse behavior is active for these tests.
        setViewportWidth(800);
        spectator = createService();
        service = spectator.service;
        globalStore = spectator.inject(GlobalStore) as unknown as typeof globalStore;
    });

    it('collapses an expanded nav on open and restores it on close', () => {
        service.acquire();

        expect(globalStore.collapseNavigation).toHaveBeenCalledTimes(1);
        expect(localStorage.getItem(LS_KEY)).toBe('false');

        service.release();

        expect(globalStore.expandNavigation).toHaveBeenCalledTimes(1);
        expect(localStorage.getItem(LS_KEY)).toBeNull();
    });

    it('keeps an already-collapsed nav collapsed (no collapse, no restore)', () => {
        globalStore.isNavigationCollapsed.mockReturnValue(true);

        service.acquire();
        expect(globalStore.collapseNavigation).not.toHaveBeenCalled();
        expect(localStorage.getItem(LS_KEY)).toBe('true');

        service.release();
        expect(globalStore.expandNavigation).not.toHaveBeenCalled();
    });

    it('ref-counts stacked panels: collapses once, restores only when the last closes', () => {
        service.acquire(); // outer
        service.acquire(); // inner (stacked)

        expect(globalStore.collapseNavigation).toHaveBeenCalledTimes(1);

        service.release(); // inner closes — nav must stay collapsed
        expect(globalStore.expandNavigation).not.toHaveBeenCalled();

        service.release(); // outer closes — now restore
        expect(globalStore.expandNavigation).toHaveBeenCalledTimes(1);
    });

    it('does nothing on a wide viewport (collapse only applies to small screens)', () => {
        setWideViewport();

        service.acquire();
        expect(globalStore.collapseNavigation).not.toHaveBeenCalled();
        expect(localStorage.getItem(LS_KEY)).toBeNull();

        service.release();
        expect(globalStore.expandNavigation).not.toHaveBeenCalled();
    });

    it('does not overwrite the persisted pre-panel state on a refresh with a panel open', () => {
        // Simulate a refresh: LS already holds the original (expanded) pre-panel state, and the nav
        // is currently collapsed (from before the refresh).
        localStorage.setItem(LS_KEY, 'false');
        globalStore.isNavigationCollapsed.mockReturnValue(true);

        service.acquire();

        // Did not re-capture (still 'false' = was expanded) and did not collapse again.
        expect(localStorage.getItem(LS_KEY)).toBe('false');
        expect(globalStore.collapseNavigation).not.toHaveBeenCalled();

        service.release();

        // Restores to the original expanded state.
        expect(globalStore.expandNavigation).toHaveBeenCalledTimes(1);
        expect(localStorage.getItem(LS_KEY)).toBeNull();
    });
});
