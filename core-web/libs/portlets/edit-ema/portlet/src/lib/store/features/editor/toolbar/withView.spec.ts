import { describe, expect } from '@jest/globals';
import { createServiceFactory, mockProvider, SpectatorService } from '@ngneat/spectator/jest';
import { patchState, signalStore, withComputed, withFeature, withState } from '@ngrx/signals';
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
import { MOCK_RESPONSE_HEADLESS, mockCurrentUser } from '../../../../shared/mocks';
import { Orientation, UVEState } from '../../../models';
import { createInitialUVEState } from '../../../testing/mocks';
import { withFlags } from '../../flags/withFlags';
import { withPage } from '../../page/withPage';

const pageParams = {
    url: 'test-url',
    language_id: '1',
    [PERSONA_KEY]: 'dot:persona',
    variantName: 'DEFAULT',
    clientHost: 'http://localhost:3000'
};

const initialState = createInitialUVEState({
    uveCurrentUser: mockCurrentUser,
    flags: {
        FEATURE_FLAG_UVE_TOGGLE_LOCK: false  // Disable toggle lock to test old unlock button behavior
    },
    pageParams
});

/** patchState type assertion - Spectator store type doesn't satisfy WritableStateSource but runtime works */
const patchStoreState = (store: unknown, state: Partial<UVEState>) => {
    patchState(store as Parameters<typeof patchState>[0], state);
};

export const uveStoreMock = signalStore(
    { protectedState: false },
    withState<UVEState>(initialState),
    withFlags([]),
    withPage(),
    // Add mock $isPageLocked computed that reacts to page state (must come before withView)
    withComputed((store) => ({
        $isPageLocked: computed(() => {
            const pageAsset = store.pageAsset();
            const page = pageAsset?.page;
            const currentUser = store.uveCurrentUser();
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
            mockProvider(DotPropertiesService, {
                getFeatureFlags: jest.fn().mockReturnValue(of(false))
            }),
            {
                provide: DotPageApiService,
                useValue: {
                    get: () => of(MOCK_RESPONSE_HEADLESS),
                    getClientPage: () => of(MOCK_RESPONSE_HEADLESS),
                    getGraphQLPage: () => of(MOCK_RESPONSE_HEADLESS),
                    save: jest.fn()
                }
            }
        ]
    });

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;
        store.setPageAssetResponse({ pageAsset: MOCK_RESPONSE_HEADLESS });
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

                    patchStoreState(store, {
                        pageExperiment: currentExperiment,
                        pageParams: {
                            ...store.pageParams(),
                            mode: UVE_MODE.EDIT
                        }
                    });
                    store.setPageAsset({
                        ...MOCK_RESPONSE_HEADLESS,
                        viewAs: {
                            ...MOCK_RESPONSE_HEADLESS.viewAs,
                            variantId: variantID
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

                    patchStoreState(store, {
                        pageExperiment: currentExperiment,
                        pageParams: {
                            ...store.pageParams(),
                            mode: UVE_MODE.PREVIEW
                        }
                    });
                    store.setPageAsset({
                        ...MOCK_RESPONSE_HEADLESS,
                        viewAs: {
                            ...MOCK_RESPONSE_HEADLESS.viewAs,
                            variantId: variantID
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

                    patchStoreState(store, {
                        pageExperiment: currentExperiment,
                        pageParams: {
                            ...store.pageParams(),
                            mode: UVE_MODE.LIVE
                        }
                    });
                    store.setPageAsset({
                        ...MOCK_RESPONSE_HEADLESS,
                        viewAs: {
                            ...MOCK_RESPONSE_HEADLESS.viewAs,
                            variantId: variantID
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
                patchStoreState(store, {
                    pageParams: {
                        ...store.pageParams(),
                        mode: UVE_MODE.PREVIEW
                    }
                });
                expect(store.$showWorkflowsActions()).toBe(false);
            });

            it('should return true when not in preview mode and is default variant', () => {
                patchStoreState(store, {
                    pageParams: {
                        ...store.pageParams(),
                        mode: UVE_MODE.EDIT
                    }
                });
                store.setPageAsset({
                    ...MOCK_RESPONSE_HEADLESS,
                    viewAs: {
                        ...MOCK_RESPONSE_HEADLESS.viewAs,
                        variantId: DEFAULT_VARIANT_ID
                    }
                });
                expect(store.$showWorkflowsActions()).toBe(true);
            });

            it('should return false when not in preview mode and is not default variant', () => {
                store.setPageAsset({
                    ...MOCK_RESPONSE_HEADLESS,
                    viewAs: {
                        ...MOCK_RESPONSE_HEADLESS.viewAs,
                        variantId: 'some-other-variant'
                    }
                });
                expect(store.$showWorkflowsActions()).toBe(false);
            });
        });

    });

    describe('methods', () => {
        describe('viewSetDevice', () => {
            it('should set the device and viewParams', () => {
                const iphone = mockDotDevices[0];

                store.viewSetDevice(iphone);
                expect(store.viewDevice()).toBe(iphone);
                expect(store.viewDeviceOrientation()).toBe(Orientation.LANDSCAPE); // This mock is on landscape, because the width is greater than the height

                expect(store.viewParams()).toEqual({
                    device: iphone.inode,
                    orientation: Orientation.LANDSCAPE,
                    seo: null
                });
            });

            it('should set the device and orientation', () => {
                const iphone = mockDotDevices[0];

                store.viewSetDevice(iphone, Orientation.PORTRAIT);
                expect(store.viewDevice()).toBe(iphone);
                expect(store.viewDeviceOrientation()).toBe(Orientation.PORTRAIT);

                expect(store.viewParams()).toEqual({
                    device: iphone.inode,
                    orientation: Orientation.PORTRAIT,
                    seo: null
                });
            });
        });

        describe('viewSetOrientation', () => {
            it('should set the orientation and the view params', () => {
                // First set a device so viewParams is not null
                store.viewSetDevice(mockDotDevices[0]);

                // Now change orientation
                store.viewSetOrientation(Orientation.PORTRAIT);
                expect(store.viewDeviceOrientation()).toBe(Orientation.PORTRAIT);

                expect(store.viewParams()).toEqual({
                    device: store.viewParams().device,
                    orientation: Orientation.PORTRAIT,
                    seo: null
                });
            });

            describe('viewClearDeviceAndSocialMedia', () => {
                // We have to extend this test because we have to test the social media
                it('should clear the device and social media', () => {
                    store.viewSetDevice(mockDotDevices[0]);

                    store.viewClearDeviceAndSocialMedia();

                    expect(store.viewDevice()).toBe(null);
                    expect(store.viewSocialMedia()).toBe(null);
                    expect(store.viewDeviceOrientation()).toBe(null);
                    expect(store.viewParams()).toEqual({
                        device: null,
                        orientation: null,
                        seo: null
                    });
                });
            });
        });

        describe('viewSetSEO', () => {
            it('should set the seo, update viewparams, and remove device and orientation', () => {
                store.viewSetSEO('seo');

                expect(store.viewSocialMedia()).toBe('seo');
                expect(store.viewDevice()).toBe(null);
                expect(store.viewDeviceOrientation()).toBe(null);

                expect(store.viewParams()).toEqual({
                    device: null,
                    orientation: null,
                    seo: 'seo'
                });
            });
        });
    });
});
