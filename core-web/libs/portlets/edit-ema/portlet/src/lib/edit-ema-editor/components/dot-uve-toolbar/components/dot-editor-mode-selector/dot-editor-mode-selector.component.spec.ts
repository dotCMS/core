import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { signal } from '@angular/core';

import { DotMessageService } from '@dotcms/data-access';
import { UVE_MODE } from '@dotcms/types';

import { DotEditorModeSelectorComponent } from './dot-editor-mode-selector.component';

import { UVEStore } from '../../../../../store/dot-uve.store';

describe('DotEditorModeSelectorComponent', () => {
    let spectator: Spectator<DotEditorModeSelectorComponent>;

    let store: {
        $hasAccessToEditMode: ReturnType<typeof signal<boolean>>;
        $isLockFeatureEnabled: ReturnType<typeof signal<boolean>>;
        pageParams: ReturnType<typeof signal<{ mode: UVE_MODE }>>;
        clearDeviceAndSocialMedia: jest.Mock;
        loadPageAsset: jest.Mock;
        trackUVEModeChange: jest.Mock;
    };

    const createComponent = createComponentFactory({
        component: DotEditorModeSelectorComponent,
        providers: [
            {
                provide: DotMessageService,
                useValue: {
                    get: (key: string) => key
                }
            }
        ]
    });

    const openSelectOverlay = () => {
        spectator.click('[data-testId="more-button"]');
        spectator.detectChanges();
    };

    beforeAll(() => {
        // PrimeNG overlays rely on matchMedia; JSDOM doesn't provide it by default.
        Object.defineProperty(window, 'matchMedia', {
            writable: true,
            value: jest.fn().mockImplementation((query: string) => ({
                matches: false,
                media: query,
                onchange: null,
                addListener: jest.fn(), // deprecated
                removeListener: jest.fn(), // deprecated
                addEventListener: jest.fn(),
                removeEventListener: jest.fn(),
                dispatchEvent: jest.fn()
            }))
        });
    });

    beforeEach(() => {
        store = {
            $hasAccessToEditMode: signal(true),
            $isLockFeatureEnabled: signal(false),
            pageParams: signal({ mode: UVE_MODE.EDIT }),
            clearDeviceAndSocialMedia: jest.fn(),
            loadPageAsset: jest.fn(),
            trackUVEModeChange: jest.fn()
        };

        spectator = createComponent({
            providers: [
                {
                    provide: UVEStore,
                    useValue: store
                }
            ]
        });
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });

    describe('template', () => {
        it('should render the mode selector', () => {
            expect(spectator.query('[data-testId="more-button"]')).toBeTruthy();
        });
    });

    describe('menu items visibility', () => {
        it('should show all 3 modes when lock feature is disabled and user can edit', () => {
            store.$isLockFeatureEnabled.set(false);
            store.$hasAccessToEditMode.set(true);
            spectator.detectChanges();

            openSelectOverlay();

            expect(
                document.querySelector(`[data-testId="${UVE_MODE.EDIT}-menu-item"]`)
            ).toBeTruthy();
            expect(
                document.querySelector(`[data-testId="${UVE_MODE.PREVIEW}-menu-item"]`)
            ).toBeTruthy();
            expect(
                document.querySelector(`[data-testId="${UVE_MODE.LIVE}-menu-item"]`)
            ).toBeTruthy();
        });

        it('should hide draft (EDIT) when lock feature is disabled and user cannot edit', () => {
            store.$isLockFeatureEnabled.set(false);
            store.$hasAccessToEditMode.set(false);
            spectator.detectChanges();

            openSelectOverlay();

            expect(document.querySelector(`[data-testId="${UVE_MODE.EDIT}-menu-item"]`)).toBeNull();
            expect(
                document.querySelector(`[data-testId="${UVE_MODE.PREVIEW}-menu-item"]`)
            ).toBeTruthy();
            expect(
                document.querySelector(`[data-testId="${UVE_MODE.LIVE}-menu-item"]`)
            ).toBeTruthy();
        });

        it('should show draft (EDIT) even when user cannot edit if lock feature is enabled', () => {
            store.$isLockFeatureEnabled.set(true);
            store.$hasAccessToEditMode.set(false);
            spectator.detectChanges();

            openSelectOverlay();

            expect(
                document.querySelector(`[data-testId="${UVE_MODE.EDIT}-menu-item"]`)
            ).toBeTruthy();
        });
    });

    describe('mode changes', () => {
        it('should call store methods when user selects a different mode', () => {
            // Start in EDIT mode
            store.pageParams.set({ mode: UVE_MODE.EDIT });
            spectator.detectChanges();

            const previewOption = spectator.component
                .$menuItems()
                .find((item) => item.id === UVE_MODE.PREVIEW);

            expect(previewOption).toBeTruthy();

            spectator.triggerEventHandler('p-select', 'onChange', { value: previewOption });
            spectator.detectChanges();

            expect(store.trackUVEModeChange).toHaveBeenCalledWith({
                fromMode: UVE_MODE.EDIT,
                toMode: UVE_MODE.PREVIEW
            });
            expect(store.loadPageAsset).toHaveBeenCalledWith({
                mode: UVE_MODE.PREVIEW,
                publishDate: undefined
            });
        });

        it('should clear device and social media when switching to EDIT mode', () => {
            // Start in PREVIEW mode
            store.pageParams.set({ mode: UVE_MODE.PREVIEW });
            spectator.detectChanges();

            const editOption = spectator.component
                .$menuItems()
                .find((item) => item.id === UVE_MODE.EDIT);

            expect(editOption).toBeTruthy();

            spectator.triggerEventHandler('p-select', 'onChange', { value: editOption });
            spectator.detectChanges();

            expect(store.clearDeviceAndSocialMedia).toHaveBeenCalledTimes(1);
            expect(store.trackUVEModeChange).toHaveBeenCalledWith({
                fromMode: UVE_MODE.PREVIEW,
                toMode: UVE_MODE.EDIT
            });
            expect(store.loadPageAsset).toHaveBeenCalledWith({
                mode: UVE_MODE.EDIT,
                publishDate: undefined
            });
        });

        it('should not call store methods when user selects the current mode', () => {
            // Start in PREVIEW mode
            store.pageParams.set({ mode: UVE_MODE.PREVIEW });
            spectator.detectChanges();

            const previewOption = spectator.component
                .$menuItems()
                .find((item) => item.id === UVE_MODE.PREVIEW);

            spectator.triggerEventHandler('p-select', 'onChange', { value: previewOption });
            spectator.detectChanges();

            expect(store.trackUVEModeChange).not.toHaveBeenCalled();
            expect(store.loadPageAsset).not.toHaveBeenCalled();
            expect(store.clearDeviceAndSocialMedia).not.toHaveBeenCalled();
        });
    });

    describe('mode guard effect (legacy behavior)', () => {
        it('should switch to PREVIEW when in EDIT without edit permission and lock feature is disabled', () => {
            store.$isLockFeatureEnabled.set(false);
            store.$hasAccessToEditMode.set(false);
            store.pageParams.set({ mode: UVE_MODE.EDIT });
            spectator.detectChanges();

            expect(store.trackUVEModeChange).toHaveBeenCalledWith({
                fromMode: UVE_MODE.EDIT,
                toMode: UVE_MODE.PREVIEW
            });
            expect(store.loadPageAsset).toHaveBeenCalledWith({
                mode: UVE_MODE.PREVIEW,
                publishDate: undefined
            });
        });

        it('should do nothing when lock feature is enabled', () => {
            store.$isLockFeatureEnabled.set(true);
            store.$hasAccessToEditMode.set(false);
            store.pageParams.set({ mode: UVE_MODE.EDIT });
            spectator.detectChanges();

            expect(store.trackUVEModeChange).not.toHaveBeenCalled();
            expect(store.loadPageAsset).not.toHaveBeenCalled();
        });
    });
});
