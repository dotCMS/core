import { describe, expect } from '@jest/globals';
import { createServiceFactory, mockProvider, SpectatorService } from '@ngneat/spectator/jest';
import { patchState, signalStore, withComputed, withFeature, withState } from '@ngrx/signals';
import { of } from 'rxjs';

import { computed } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { DotPropertiesService } from '@dotcms/data-access';
import { DEFAULT_VARIANT_ID, DEFAULT_VARIANT_NAME, DotDevice } from '@dotcms/dotcms-models';
import { UVE_MODE } from '@dotcms/types';
import { getRunningExperimentMock, mockDotDevices } from '@dotcms/utils-testing';

import { withView } from './withView';

import { DotPageApiService } from '../../../../services/dot-page-api/dot-page-api.service';
import {
    DEFAULT_DEVICE,
    DEFAULT_DEVICES,
    DEFAULT_PERSONA,
    MIN_IFRAME_HEIGHT,
    MIN_IFRAME_WIDTH,
    PERSONA_KEY
} from '../../../../shared/consts';
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
        FEATURE_FLAG_UVE_TOGGLE_LOCK: false // Disable toggle lock to test old unlock button behavior
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
    withFeature(() => withView())
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
        store.setPageAsset({ pageAsset: MOCK_RESPONSE_HEADLESS });
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
                        pageAsset: {
                            ...MOCK_RESPONSE_HEADLESS,
                            viewAs: {
                                ...MOCK_RESPONSE_HEADLESS.viewAs,
                                variantId: variantID
                            }
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
                        pageAsset: {
                            ...MOCK_RESPONSE_HEADLESS,
                            viewAs: {
                                ...MOCK_RESPONSE_HEADLESS.viewAs,
                                variantId: variantID
                            }
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
                        pageAsset: {
                            ...MOCK_RESPONSE_HEADLESS,
                            viewAs: {
                                ...MOCK_RESPONSE_HEADLESS.viewAs,
                                variantId: variantID
                            }
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
                    pageAsset: {
                        ...MOCK_RESPONSE_HEADLESS,
                        viewAs: {
                            ...MOCK_RESPONSE_HEADLESS.viewAs,
                            variantId: DEFAULT_VARIANT_ID
                        }
                    }
                });
                expect(store.$showWorkflowsActions()).toBe(true);
            });

            it('should return false when not in preview mode and is not default variant', () => {
                store.setPageAsset({
                    pageAsset: {
                        ...MOCK_RESPONSE_HEADLESS,
                        viewAs: {
                            ...MOCK_RESPONSE_HEADLESS.viewAs,
                            variantId: 'some-other-variant'
                        }
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

    describe('Iframe sizing & zoom (DevTools-style)', () => {
        const TABLET = DEFAULT_DEVICES.find((d) => d.inode === 'tablet') as DotDevice;

        beforeEach(() => {
            // Provide a known canvas size so clamps and fits have something to bite.
            patchStoreState(store, {
                viewCanvasAvailableWidth: 1200,
                viewCanvasAvailableHeight: 800
            });
        });

        describe('zoom computeds', () => {
            it('$viewZoomFactor returns viewZoomLevel / 100', () => {
                patchStoreState(store, { viewZoomLevel: 75 });
                expect(store.$viewZoomFactor()).toBe(0.75);
            });

            it('viewZoomLevel exposes the raw integer level', () => {
                patchStoreState(store, { viewZoomLevel: 150 });
                expect(store.viewZoomLevel()).toBe(150);
            });
        });

        describe('$viewIsResponsiveMode', () => {
            it('is true when viewDevice is null', () => {
                patchStoreState(store, { viewDevice: null });
                expect(store.$viewIsResponsiveMode()).toBe(true);
            });

            it('is true for the default device', () => {
                patchStoreState(store, { viewDevice: DEFAULT_DEVICE });
                expect(store.$viewIsResponsiveMode()).toBe(true);
            });

            it('is false for a non-default device', () => {
                patchStoreState(store, { viewDevice: TABLET });
                expect(store.$viewIsResponsiveMode()).toBe(false);
            });
        });

        describe('$viewCanvasOuterStyles', () => {
            it('mirrors viewIframeWidth/Height in px', () => {
                patchStoreState(store, { viewIframeWidth: 1024, viewIframeHeight: 600 });
                expect(store.$viewCanvasOuterStyles()).toEqual({
                    width: '1024px',
                    height: '600px'
                });
            });

            it('falls back to 100% when iframe size is 0 (initial / pre-load)', () => {
                patchStoreState(store, { viewIframeWidth: 0, viewIframeHeight: 0 });
                expect(store.$viewCanvasOuterStyles()).toEqual({
                    width: '100%',
                    height: '100%'
                });
            });
        });

        describe('$viewCanvasInnerStyles', () => {
            it('inverts zoom on inner size and applies transform: scale', () => {
                patchStoreState(store, {
                    viewIframeWidth: 800,
                    viewIframeHeight: 600,
                    viewZoomLevel: 50
                });
                expect(store.$viewCanvasInnerStyles()).toEqual({
                    width: '1600px', // 800 / 0.5
                    height: '1200px', // 600 / 0.5
                    transform: 'scale(0.5)',
                    transformOrigin: 'top left'
                });
            });
        });

        describe('viewSetIframeSize', () => {
            it('clamps width to canvas in responsive mode', () => {
                store.viewSetIframeSize({ width: 5000 });
                expect(store.viewIframeWidth()).toBe(1200);
            });

            it('clamps width to MIN_IFRAME_WIDTH', () => {
                store.viewSetIframeSize({ width: 100 });
                expect(store.viewIframeWidth()).toBe(MIN_IFRAME_WIDTH);
            });

            it('clamps height to canvas in responsive mode', () => {
                store.viewSetIframeSize({ height: 5000 });
                expect(store.viewIframeHeight()).toBe(800);
            });

            it('clamps height to MIN_IFRAME_HEIGHT', () => {
                store.viewSetIframeSize({ height: 50 });
                expect(store.viewIframeHeight()).toBe(MIN_IFRAME_HEIGHT);
            });

            it('rounds floats', () => {
                store.viewSetIframeSize({ width: 800.6, height: 600.2 });
                expect(store.viewIframeWidth()).toBe(801);
                expect(store.viewIframeHeight()).toBe(600);
            });

            it('does not cap when in device mode (only MIN floor applies)', () => {
                patchStoreState(store, { viewDevice: TABLET });
                store.viewSetIframeSize({ width: 5000, height: 5000 });
                expect(store.viewIframeWidth()).toBe(5000);
                expect(store.viewIframeHeight()).toBe(5000);
            });

            it('leaves the unspecified axis untouched', () => {
                patchStoreState(store, { viewIframeWidth: 700, viewIframeHeight: 500 });
                store.viewSetIframeSize({ width: 800 });
                expect(store.viewIframeWidth()).toBe(800);
                expect(store.viewIframeHeight()).toBe(500);
            });
        });

        describe('viewResizeIframe', () => {
            it('exits the device preset and applies the new size atomically', () => {
                store.viewSetDevice(TABLET);
                expect(store.viewDevice()?.inode).toBe('tablet');

                store.viewResizeIframe({ width: 900 });

                expect(store.viewDevice()).toBeNull();
                expect(store.viewIframeWidth()).toBe(900);
            });

            it('is a no-op on the device side when already responsive', () => {
                patchStoreState(store, { viewDevice: null, viewIframeWidth: 700 });
                store.viewResizeIframe({ width: 850 });

                expect(store.viewDevice()).toBeNull();
                expect(store.viewIframeWidth()).toBe(850);
            });
        });

        describe('viewSetCanvasAvailableSize', () => {
            it('rounds the values', () => {
                store.viewSetCanvasAvailableSize({ width: 1234.7, height: 567.4 });
                expect(store.viewCanvasAvailableWidth()).toBe(1235);
                expect(store.viewCanvasAvailableHeight()).toBe(567);
            });

            it('clamps negative values to 0', () => {
                store.viewSetCanvasAvailableSize({ width: -10, height: -20 });
                expect(store.viewCanvasAvailableWidth()).toBe(0);
                expect(store.viewCanvasAvailableHeight()).toBe(0);
            });
        });

        describe('viewSetDevice (size + zoom side-effects)', () => {
            it('snaps to canvas at 100% zoom when picking the default device', () => {
                patchStoreState(store, {
                    viewDevice: TABLET,
                    viewIframeWidth: 555,
                    viewIframeHeight: 799,
                    viewZoomLevel: 67
                });
                store.viewSetDevice(DEFAULT_DEVICE);

                expect(store.viewIframeWidth()).toBe(1200);
                expect(store.viewIframeHeight()).toBe(800);
                expect(store.viewZoomLevel()).toBe(100);
                expect(store.viewDevice()?.inode).toBe('default');
            });

            it('fits a non-default device to the canvas with auto-zoom', () => {
                // Tablet portrait is 820 × 1180; canvas is 1200 × 800.
                // fit = min(1, 1200/820, 800/1180) = 800/1180 ≈ 0.6779
                store.viewSetDevice(TABLET);

                expect(store.viewDevice()?.inode).toBe('tablet');
                expect(store.viewZoomLevel()).toBe(68);
                expect(store.viewIframeHeight()).toBe(800);
                expect(store.viewIframeWidth()).toBe(556);
            });

            it('respects the orientation argument', () => {
                store.viewSetDevice(TABLET, Orientation.LANDSCAPE);

                expect(store.viewDeviceOrientation()).toBe(Orientation.LANDSCAPE);
                expect(store.viewIframeHeight()).toBe(800);
                // Landscape tablet: 1180×820; canvas 1200×800; fit = 800/820 ≈ 0.9756
                expect(store.viewIframeWidth()).toBe(Math.round(1180 * (800 / 820)));
            });

            it('keeps the simulated viewport correct when canvas is smaller than minimum zoom', () => {
                // Canvas 100×100 is far too small for an 820×1180 tablet.
                // Raw fit would be ~0.085 — below the 10% zoom floor.
                // The contract: iframe / zoom must equal the device's CSS
                // dimensions so the page inside renders at the right viewport.
                patchStoreState(store, {
                    viewCanvasAvailableWidth: 100,
                    viewCanvasAvailableHeight: 100
                });
                store.viewSetDevice(TABLET);

                // Zoom is clamped to 10 (the minimum).
                expect(store.viewZoomLevel()).toBe(10);
                // iframe / zoomFactor must equal the device's CSS dims.
                const zoomFactor = store.viewZoomLevel() / 100;
                expect(store.viewIframeWidth() / zoomFactor).toBeCloseTo(820, 0);
                expect(store.viewIframeHeight() / zoomFactor).toBeCloseTo(1180, 0);
            });
        });

        describe('viewSetOrientation (refits in device mode)', () => {
            it('refits the iframe when a non-default device is active', () => {
                store.viewSetDevice(TABLET); // portrait
                store.viewSetOrientation(Orientation.LANDSCAPE);

                expect(store.viewDeviceOrientation()).toBe(Orientation.LANDSCAPE);
                expect(store.viewIframeHeight()).toBe(800);
                expect(store.viewIframeWidth()).toBe(Math.round(1180 * (800 / 820)));
            });

            it('does not change size when on the default device', () => {
                patchStoreState(store, {
                    viewDevice: DEFAULT_DEVICE,
                    viewIframeWidth: 700,
                    viewIframeHeight: 500
                });
                store.viewSetOrientation(Orientation.LANDSCAPE);

                expect(store.viewIframeWidth()).toBe(700);
                expect(store.viewIframeHeight()).toBe(500);
            });
        });

        describe('viewExitDevicePreset', () => {
            it('is a no-op in responsive mode', () => {
                patchStoreState(store, {
                    viewDevice: null,
                    viewIframeWidth: 700,
                    viewIframeHeight: 500,
                    viewZoomLevel: 80
                });
                store.viewExitDevicePreset();

                expect(store.viewDevice()).toBeNull();
                expect(store.viewIframeWidth()).toBe(700);
                expect(store.viewIframeHeight()).toBe(500);
                expect(store.viewZoomLevel()).toBe(80);
            });

            it('clears the device flag without changing size or zoom', () => {
                store.viewSetDevice(TABLET);
                const widthBefore = store.viewIframeWidth();
                const heightBefore = store.viewIframeHeight();
                const zoomBefore = store.viewZoomLevel();

                store.viewExitDevicePreset();

                expect(store.viewDevice()).toBeNull();
                expect(store.viewDeviceOrientation()).toBeNull();
                expect(store.viewIframeWidth()).toBe(widthBefore);
                expect(store.viewIframeHeight()).toBe(heightBefore);
                expect(store.viewZoomLevel()).toBe(zoomBefore);
            });
        });

        describe('zoom methods', () => {
            it('viewZoomSetLevel clamps to [10, 300]', () => {
                store.viewZoomSetLevel(5);
                expect(store.viewZoomLevel()).toBe(10);

                store.viewZoomSetLevel(500);
                expect(store.viewZoomLevel()).toBe(300);
            });

            it('viewZoomSetLevel does not change iframe size', () => {
                patchStoreState(store, { viewIframeWidth: 800, viewIframeHeight: 600 });
                store.viewZoomSetLevel(200);
                expect(store.viewIframeWidth()).toBe(800);
                expect(store.viewIframeHeight()).toBe(600);
            });

            it('viewZoomReset resets zoom and snaps iframe to canvas in responsive mode', () => {
                patchStoreState(store, {
                    viewZoomLevel: 50,
                    viewIframeWidth: 600,
                    viewIframeHeight: 400
                });
                store.viewZoomReset();

                expect(store.viewZoomLevel()).toBe(100);
                expect(store.viewIframeWidth()).toBe(1200);
                expect(store.viewIframeHeight()).toBe(800);
            });

            it('viewZoomReset only resets zoom in device mode', () => {
                store.viewSetDevice(TABLET);
                const widthBefore = store.viewIframeWidth();
                const heightBefore = store.viewIframeHeight();

                store.viewZoomReset();

                expect(store.viewZoomLevel()).toBe(100);
                expect(store.viewIframeWidth()).toBe(widthBefore);
                expect(store.viewIframeHeight()).toBe(heightBefore);
            });

            it('viewZoomReset does not snap when canvas size is unknown', () => {
                patchStoreState(store, {
                    viewCanvasAvailableWidth: 0,
                    viewCanvasAvailableHeight: 0,
                    viewIframeWidth: 600,
                    viewIframeHeight: 400,
                    viewZoomLevel: 50
                });
                store.viewZoomReset();

                expect(store.viewZoomLevel()).toBe(100);
                expect(store.viewIframeWidth()).toBe(600);
                expect(store.viewIframeHeight()).toBe(400);
            });

            it('viewZoomLabel formats the zoom level as a percentage', () => {
                patchStoreState(store, { viewZoomLevel: 175 });
                expect(store.viewZoomLabel()).toBe('175%');
            });
        });
    });
});
