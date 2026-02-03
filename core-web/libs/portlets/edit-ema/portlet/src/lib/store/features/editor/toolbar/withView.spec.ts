import { expect, describe } from '@jest/globals';
import { createServiceFactory, mockProvider, SpectatorService } from '@ngneat/spectator/jest';
import { patchState, signalStore, withState, withComputed, withFeature } from '@ngrx/signals';
import { of } from 'rxjs';

import { computed } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { DotPropertiesService } from '@dotcms/data-access';
import { DEFAULT_VARIANT_ID, DEFAULT_VARIANT_NAME } from '@dotcms/dotcms-models';
import { UVE_MODE } from '@dotcms/types';
import { getRunningExperimentMock, mockDotDevices } from '@dotcms/utils-testing';

import { withView } from './withView';

import { DotPageApiService } from '../../../../services/dot-page-api.service';
import { DEFAULT_PERSONA, PERSONA_KEY } from '../../../../shared/consts';
import { EDITOR_STATE, UVE_STATUS } from '../../../../shared/enums';
import { MOCK_RESPONSE_HEADLESS, mockCurrentUser } from '../../../../shared/mocks';
import { Orientation, PageType, UVEState } from '../../../models';

const pageParams = {
    url: 'test-url',
    language_id: '1',
    [PERSONA_KEY]: 'dot:persona',
    variantName: 'DEFAULT',
    clientHost: 'http://localhost:3000'
};

const initialState: UVEState = {
    isEnterprise: true,
    languages: [],
    flags: {
        FEATURE_FLAG_UVE_TOGGLE_LOCK: false  // Disable toggle lock to test old unlock button behavior
    },
    currentUser: mockCurrentUser,
    experiment: null,
    errorCode: null,
    pageParams,
    status: UVE_STATUS.LOADED,
    pageType: PageType.HEADLESS,
    // Normalized page response properties
    page: MOCK_RESPONSE_HEADLESS.page,
    site: MOCK_RESPONSE_HEADLESS.site,
    viewAs: MOCK_RESPONSE_HEADLESS.viewAs,
    template: MOCK_RESPONSE_HEADLESS.template,
    layout: MOCK_RESPONSE_HEADLESS.layout,
    urlContentMap: MOCK_RESPONSE_HEADLESS.urlContentMap,
    containers: MOCK_RESPONSE_HEADLESS.containers,
    vanityUrl: MOCK_RESPONSE_HEADLESS.vanityUrl,
    numberContents: MOCK_RESPONSE_HEADLESS.numberContents,
    // Phase 3: Nested editor state
    editor: {
        dragItem: null,
        bounds: [],
        state: EDITOR_STATE.IDLE,
        activeContentlet: null,
        contentArea: null,
        panels: {
            palette: { open: true },
            rightSidebar: { open: false }
        },
        ogTags: null,
        styleSchemas: []
    },
    // Phase 3: Nested view state
    view: {
        device: null,
        orientation: Orientation.LANDSCAPE,
        socialMedia: null,
        viewParams: null,
        isEditState: true,
        isPreviewModeActive: false,
        ogTagsResults: null
    }
};

export const uveStoreMock = signalStore(
    { protectedState: false },
    withState<UVEState>(initialState),
    // Add mock $isPageLocked computed that reacts to page state (must come before withView)
    withComputed((store) => ({
        $isPageLocked: computed(() => {
            const page = store.page();
            const currentUser = store.currentUser();
            const isLockedByOther = page?.locked && page?.lockedBy !== currentUser?.userId;
            return isLockedByOther || false;
        })
    })),
    // Use withFeature to access store and pass reactive dependency
    withFeature((store) => withView({
        $isPageLocked: () => store.$isPageLocked()  // Call the computed above
    }))
);

describe('withView', () => {
    let spectator: SpectatorService<InstanceType<typeof uveStoreMock>>;
    let store: InstanceType<typeof uveStoreMock>;

    const createService = createServiceFactory({
        service: uveStoreMock,
        providers: [
            mockProvider(Router),
            mockProvider(ActivatedRoute),
            mockProvider(Router),
            mockProvider(DotPropertiesService, {
                getFeatureFlags: jest.fn().mockReturnValue(of(false))
            }),
            {
                provide: DotPageApiService,
                useValue: {
                    get() {
                        return of(MOCK_RESPONSE_HEADLESS);
                    },
                    getClientPage() {
                        return of(MOCK_RESPONSE_HEADLESS);
                    },
                    save: jest.fn()
                }
            }
        ]
    });

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;
    });

    describe('Computed', () => {
        it('should return the right API URL', () => {
            const params = { ...pageParams };

            // Delete the url from the params to test the function
            delete params.url;
            delete params.clientHost;

            const queryParams = new URLSearchParams(params).toString();
            const expectURL = `/api/v1/page/json/test-url?${queryParams}`;

            expect(store.$apiURL()).toBe(expectURL);
        });

        it('should return the personaSelector props', () => {
            expect(store.$personaSelector()).toEqual({
                pageId: '123',
                value: DEFAULT_PERSONA
            });
        });

        // We don't need to test all the interactions of this because we already test this in the proper component
        it('should return the right infoDisplayProps', () => {
            expect(store.$infoDisplayProps()).toEqual(null);
        });

        describe('$infoDisplayProps', () => {
            it('should return null', () => {
                expect(store.$infoDisplayProps()).toBe(null);
            });
            describe('variant', () => {
                it('should show have text for variant when in edit mode', () => {
                    const currentExperiment = getRunningExperimentMock();

                    const variantID = currentExperiment.trafficProportion.variants.find(
                        (variant) => variant.name !== DEFAULT_VARIANT_NAME
                    ).id;

                    patchState(store, {
                        viewAs: {
                            ...MOCK_RESPONSE_HEADLESS.viewAs,
                            variantId: variantID
                        },
                        experiment: currentExperiment,
                        pageParams: {
                            ...store.pageParams(),
                            mode: UVE_MODE.EDIT
                        }
                    });

                    expect(store.$infoDisplayProps()).toEqual({
                        icon: 'pi pi-file-edit',
                        id: 'variant',
                        info: {
                            message: 'editpage.editing.variant',
                            args: ['Variant A']
                        },
                        actionIcon: 'pi pi-arrow-left'
                    });
                });
                it('should show have text for variant when in preview mode', () => {
                    const currentExperiment = getRunningExperimentMock();

                    const variantID = currentExperiment.trafficProportion.variants.find(
                        (variant) => variant.name !== DEFAULT_VARIANT_NAME
                    ).id;

                    patchState(store, {
                        viewAs: {
                            ...MOCK_RESPONSE_HEADLESS.viewAs,
                            variantId: variantID
                        },
                        experiment: currentExperiment,
                        pageParams: {
                            ...store.pageParams(),
                            mode: UVE_MODE.PREVIEW
                        }
                    });

                    expect(store.$infoDisplayProps()).toEqual({
                        icon: 'pi pi-file-edit',
                        id: 'variant',
                        info: {
                            message: 'editpage.viewing.variant',
                            args: ['Variant A']
                        },
                        actionIcon: 'pi pi-arrow-left'
                    });
                });

                it('should show have text for variant when in live mode', () => {
                    const currentExperiment = getRunningExperimentMock();

                    const variantID = currentExperiment.trafficProportion.variants.find(
                        (variant) => variant.name !== DEFAULT_VARIANT_NAME
                    ).id;

                    patchState(store, {
                        viewAs: {
                            ...MOCK_RESPONSE_HEADLESS.viewAs,
                            variantId: variantID
                        },
                        experiment: currentExperiment,
                        pageParams: {
                            ...store.pageParams(),
                            mode: UVE_MODE.LIVE
                        }
                    });

                    expect(store.$infoDisplayProps()).toEqual({
                        icon: 'pi pi-file-edit',
                        id: 'variant',
                        info: {
                            message: 'editpage.viewing.variant',
                            args: ['Variant A']
                        },
                        actionIcon: 'pi pi-arrow-left'
                    });
                });
            });
        });
        describe('$showWorkflowsActions', () => {
            it('should return false when in preview mode', () => {
                patchState(store, {
                    pageParams: {
                        ...store.pageParams(),
                        mode: UVE_MODE.PREVIEW
                    }
                });
                expect(store.$showWorkflowsActions()).toBe(false);
            });

            it('should return true when not in preview mode and is default variant', () => {
                patchState(store, {
                    pageParams: {
                        ...store.pageParams(),
                        mode: UVE_MODE.EDIT
                    },
                    viewAs: {
                        ...store.viewAs(),
                        variantId: DEFAULT_VARIANT_ID
                    }
                });
                expect(store.$showWorkflowsActions()).toBe(true);
            });

            it('should return false when not in preview mode and is not default variant', () => {
                patchState(store, {
                    viewAs: {
                        ...store.viewAs(),
                        variantId: 'some-other-variant'
                    }
                });
                expect(store.$showWorkflowsActions()).toBe(false);
            });
        });

        describe('$unlockButton', () => {
            it('should be null if the page is not locked', () => {
                patchState(store, {
                    page: {
                        ...store.page(),
                        locked: false
                    }
                });

                expect(store.$unlockButton()).toBe(null);
            });

            it('should be null if the page is locked by the current user', () => {
                patchState(store, {
                    page: {
                        ...store.page(),
                        locked: true,
                        lockedBy: mockCurrentUser.userId
                    },
                    pageParams: {
                        ...store.pageParams(),
                        mode: UVE_MODE.EDIT
                    }
                });

                expect(store.$unlockButton()).toBe(null);
            });

            it('should return the unlock button if the page is locked but mode is preview', () => {
                patchState(store, {
                    page: {
                        ...store.page(),
                        locked: true,
                        lockedBy: '123',
                        lockedByName: 'John Doe'
                    },
                    pageParams: {
                        ...store.pageParams(),
                        mode: UVE_MODE.PREVIEW
                    }
                });

                expect(store.$unlockButton()).toEqual({
                    inode: store.page().inode,
                    disabled: false,
                    loading: false,
                    info: {
                        message: 'editpage.toolbar.page.release.lock.locked.by.user',
                        args: ['John Doe']
                    }
                });
            });

            it('should return the unlock button if the page is locked but mode is live', () => {
                patchState(store, {
                    page: {
                        ...store.page(),
                        locked: true,
                        lockedBy: '123',
                        lockedByName: 'John Doe'
                    },
                    pageParams: {
                        ...store.pageParams(),
                        mode: UVE_MODE.LIVE
                    }
                });

                expect(store.$unlockButton()).toEqual({
                    inode: store.page().inode,
                    disabled: false,
                    loading: false,
                    info: {
                        message: 'editpage.toolbar.page.release.lock.locked.by.user',
                        args: ['John Doe']
                    }
                });
            });

            it('should show label and icon when page is lock for editing and has unlock permission', () => {
                patchState(store, {
                    page: {
                        ...MOCK_RESPONSE_HEADLESS.page,
                        locked: true,
                        canLock: true,
                        lockedByName: 'John Doe',
                        lockedBy: '456'
                    },
                    pageParams: {
                        ...store.pageParams(),
                        mode: UVE_MODE.EDIT
                    },
                    status: UVE_STATUS.LOADED
                });
                expect(store.$unlockButton()).toEqual({
                    inode: store.page().inode,
                    disabled: false,
                    loading: false,
                    info: {
                        message: 'editpage.toolbar.page.release.lock.locked.by.user',
                        args: ['John Doe']
                    }
                });
            });

            it('should be disabled if the page is locked by another user and cannot be unlocked', () => {
                patchState(store, {
                    page: {
                        ...MOCK_RESPONSE_HEADLESS.page,
                        locked: true,
                        lockedBy: '123',
                        lockedByName: 'John Doe',
                        canLock: false
                    },
                    pageParams: {
                        ...store.pageParams(),
                        mode: UVE_MODE.EDIT
                    },
                    status: UVE_STATUS.LOADED
                });

                expect(store.$unlockButton()).toEqual({
                    disabled: true,
                    info: {
                        message: 'editpage.locked-by',
                        args: ['John Doe']
                    },
                    inode: store.page().inode,
                    loading: false
                });
            });
        });
    });

    describe('methods', () => {
        describe('setDevice', () => {
            it('should set the device and viewParams', () => {
                const iphone = mockDotDevices[0];

                store.setDevice(iphone);
                expect(store.view().device).toBe(iphone);
                expect(store.view().orientation).toBe(Orientation.LANDSCAPE); // This mock is on landscape, because the width is greater than the height

                expect(store.view().viewParams).toEqual({
                    device: iphone.inode,
                    orientation: Orientation.LANDSCAPE,
                    seo: null
                });
            });

            it('should set the device and orientation', () => {
                const iphone = mockDotDevices[0];

                store.setDevice(iphone, Orientation.PORTRAIT);
                expect(store.view().device).toBe(iphone);
                expect(store.view().orientation).toBe(Orientation.PORTRAIT);

                expect(store.view().viewParams).toEqual({
                    device: iphone.inode,
                    orientation: Orientation.PORTRAIT,
                    seo: null
                });
            });
        });

        describe('setOrientation', () => {
            it('should set the orientation and the view params', () => {
                // First set a device so viewParams is not null
                store.setDevice(mockDotDevices[0]);

                // Now change orientation
                store.setOrientation(Orientation.PORTRAIT);
                expect(store.view().orientation).toBe(Orientation.PORTRAIT);

                expect(store.view().viewParams).toEqual({
                    device: store.view().viewParams.device,
                    orientation: Orientation.PORTRAIT,
                    seo: null
                });
            });

            describe('clearDeviceAndSocialMedia', () => {
                // We have to extend this test because we have to test the social media
                it('should clear the device and social media', () => {
                    store.setDevice(mockDotDevices[0]);

                    store.clearDeviceAndSocialMedia();

                    expect(store.view().device).toBe(null);
                    expect(store.view().socialMedia).toBe(null);
                    expect(store.view().isEditState).toBe(true);
                    expect(store.view().orientation).toBe(null);
                    expect(store.view().viewParams).toEqual({
                        device: null,
                        orientation: null,
                        seo: null
                    });
                });
            });
        });

        describe('setSEO', () => {
            it('should set the seo, update viewparams, and remove device and orientation', () => {
                store.setSEO('seo');

                expect(store.view().socialMedia).toBe('seo');
                expect(store.view().device).toBe(null);
                expect(store.view().orientation).toBe(null);

                expect(store.view().viewParams).toEqual({
                    device: null,
                    orientation: null,
                    seo: 'seo'
                });
            });
        });
    });
});
