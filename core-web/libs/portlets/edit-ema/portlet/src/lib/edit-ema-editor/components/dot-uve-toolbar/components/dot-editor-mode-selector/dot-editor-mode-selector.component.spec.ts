import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { signal } from '@angular/core';

import { DotMessageService } from '@dotcms/data-access';
import { UVE_MODE } from '@dotcms/uve';

import { DotEditorModeSelectorComponent } from './dot-editor-mode-selector.component';

import { MOCK_RESPONSE_HEADLESS } from '../../../../../shared/mocks';
import { UVEStore } from '../../../../../store/dot-uve.store';

const pageParams = {
    url: 'test-url',
    language_id: 'en',
    'com.dotmarketing.persona.id': 'modes.persona.no.persona',
    editorMode: UVE_MODE.EDIT
};

describe('DotEditorModeSelectorComponent', () => {
    let spectator: Spectator<DotEditorModeSelectorComponent>;
    let component: DotEditorModeSelectorComponent;

    const mockStoreState = {
        canEditPage: true,
        pageAPIResponse: {
            ...MOCK_RESPONSE_HEADLESS,
            page: {
                ...MOCK_RESPONSE_HEADLESS.page,
                live: true
            }
        },
        pageParams
    };

    const mockStore = {
        canEditPage: signal(mockStoreState.canEditPage),
        pageAPIResponse: signal(mockStoreState.pageAPIResponse),
        pageParams: signal(mockStoreState.pageParams),
        clearDeviceAndSocialMedia: jest.fn(),
        loadPageAsset: jest.fn()
    };

    const createComponent = createComponentFactory({
        component: DotEditorModeSelectorComponent,
        providers: [
            {
                provide: UVEStore,
                useValue: mockStore
            },
            {
                provide: DotMessageService,
                useValue: {
                    get: (key: string) => key
                }
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
        component = spectator.component;
        jest.resetAllMocks();
    });
    describe('$menuItems', () => {
        it('should include all modes when user can edit and page has live version', () => {
            const menuItems = component.$menuItems();
            expect(menuItems).toHaveLength(3);
            expect(menuItems.map((item) => item.id)).toContain(UVE_MODE.EDIT);
            expect(menuItems.map((item) => item.id)).toContain(UVE_MODE.PREVIEW);
            expect(menuItems.map((item) => item.id)).toContain(UVE_MODE.LIVE);
        });

        it('should exclude EDIT mode when user cannot edit page', () => {
            mockStore.canEditPage.set(false);
            spectator.detectChanges();
            const menuItems = component.$menuItems();
            expect(menuItems).toHaveLength(2);
            expect(menuItems.map((item) => item.id)).not.toContain(UVE_MODE.EDIT);
        });

        it('should exclude LIVE mode when page has no live version', () => {
            mockStore.pageAPIResponse.set({
                ...MOCK_RESPONSE_HEADLESS,
                page: {
                    ...MOCK_RESPONSE_HEADLESS.page,
                    live: false
                }
            });
            mockStore.canEditPage.set(true);

            spectator.detectChanges();
            const menuItems = component.$menuItems();
            expect(menuItems).toHaveLength(2);
            expect(menuItems.map((item) => item.id)).not.toContain(UVE_MODE.LIVE);
        });
    });

    describe('$currentModeLabel', () => {
        it('should return correct label for current mode', () => {
            mockStore.pageParams.set({ ...pageParams, editorMode: UVE_MODE.PREVIEW });
            expect(component.$currentModeLabel()).toBe('uve.editor.mode.preview');
        });
    });

    describe('onModeChange', () => {
        it('should not call loadPageAsset when selected mode is current mode', () => {
            component.onModeChange(UVE_MODE.PREVIEW);
            expect(mockStore.loadPageAsset).not.toHaveBeenCalled();
        });

        it('should clear device and social media when switching to EDIT mode', () => {
            component.onModeChange(UVE_MODE.EDIT);
            expect(mockStore.clearDeviceAndSocialMedia).toHaveBeenCalled();
            expect(mockStore.loadPageAsset).toHaveBeenCalledWith({
                editorMode: UVE_MODE.EDIT,
                publishDate: undefined
            });
        });

        it('should include publishDate when switching to LIVE mode', () => {
            jest.useFakeTimers();
            const now = new Date();
            jest.setSystemTime(now);

            component.onModeChange(UVE_MODE.LIVE);
            expect(mockStore.loadPageAsset).toHaveBeenCalledWith({
                editorMode: UVE_MODE.LIVE,
                publishDate: now.toISOString()
            });

            jest.useRealTimers();
        });
    });

    describe('$modeGuardEffect', () => {
        it('should switch to PREVIEW mode when in EDIT mode without edit permission', () => {
            mockStore.pageParams.set({ ...pageParams, editorMode: UVE_MODE.EDIT });
            mockStore.canEditPage.set(false);

            spectator.detectChanges();

            expect(mockStore.loadPageAsset).toHaveBeenCalledWith({
                editorMode: UVE_MODE.PREVIEW,
                publishDate: undefined
            });
        });

        it('should switch to PREVIEW mode when in LIVE mode without live version', () => {
            mockStore.pageParams.set({ ...pageParams, editorMode: UVE_MODE.LIVE });
            mockStore.pageAPIResponse.set({
                ...MOCK_RESPONSE_HEADLESS,
                page: {
                    ...MOCK_RESPONSE_HEADLESS.page,
                    live: false
                }
            });

            spectator.detectChanges();

            expect(mockStore.loadPageAsset).toHaveBeenCalledWith({
                editorMode: UVE_MODE.PREVIEW,
                publishDate: undefined
            });
        });
    });

    describe('Menu Interactions', () => {
        beforeEach(() => {
            // Reset the store state
            mockStore.canEditPage.set(true);
            mockStore.pageAPIResponse.set(MOCK_RESPONSE_HEADLESS);
            mockStore.pageParams.set(pageParams);
        });

        it('should show menu when clicking the button', () => {
            const button = spectator.query('[data-testId="more-button"]');
            spectator.click(button);

            const menu = spectator.query('[data-testId="more-menu"]');
            expect(menu).toBeVisible();
        });

        it('should change mode when clicking a menu item', () => {
            // Setup initial state as EDIT mode
            mockStore.pageParams.set({ ...pageParams, editorMode: UVE_MODE.EDIT });
            spectator.detectChanges();

            // Open menu
            const button = spectator.query('[data-testId="more-button"]');
            spectator.click(button);
            spectator.detectChanges();

            // Click the Preview mode menu item
            const menuItems = spectator.queryAll('.menu-item');
            const previewMenuItem = menuItems[1]; // Preview is second item
            spectator.click(previewMenuItem);

            expect(mockStore.loadPageAsset).toHaveBeenCalledWith({
                editorMode: UVE_MODE.PREVIEW,
                publishDate: undefined
            });
        });

        it('should highlight active mode in menu', () => {
            mockStore.pageParams.set({ ...pageParams, editorMode: UVE_MODE.PREVIEW });
            spectator.detectChanges();

            const button = spectator.query('[data-testId="more-button"]');
            spectator.click(button);
            spectator.detectChanges();

            const activeMenuItem = spectator.query('.menu-item--active');
            expect(activeMenuItem.querySelector('.menu-item__label')).toHaveText(
                'uve.editor.mode.preview'
            );
        });

        test.each([
            {
                mode: UVE_MODE.EDIT,
                label: 'uve.editor.mode.edit',
                description: 'uve.editor.mode.edit.description'
            },
            {
                mode: UVE_MODE.PREVIEW,
                label: 'uve.editor.mode.preview',
                description: 'uve.editor.mode.preview.description'
            },
            {
                mode: UVE_MODE.LIVE,
                label: 'uve.editor.mode.live',
                description: 'uve.editor.mode.live.description'
            }
        ])(
            'should show correct label and description for $mode menu item',
            ({ label, description, mode }) => {
                const button = spectator.query('[data-testId="more-button"]');
                spectator.click(button);
                spectator.detectChanges();

                const menuItem = spectator.query(`[data-testId="${mode}-menu-item"]`);

                expect(menuItem.querySelector('.menu-item__label')).toHaveText(label);
                expect(menuItem.querySelector('.menu-item__description')).toHaveText(description);
            }
        );

        test.each([
            { mode: UVE_MODE.EDIT, label: 'uve.editor.mode.edit' },
            { mode: UVE_MODE.PREVIEW, label: 'uve.editor.mode.preview' },
            { mode: UVE_MODE.LIVE, label: 'uve.editor.mode.live' }
        ])('should update button label when mode changes - $mode', ({ mode, label }) => {
            // Start with Edit mode
            mockStore.pageParams.set({ ...pageParams, editorMode: mode });
            spectator.detectChanges();

            const button = spectator.query('[data-testId="more-button"]');
            expect(button).toHaveText(label);
        });
    });
});
