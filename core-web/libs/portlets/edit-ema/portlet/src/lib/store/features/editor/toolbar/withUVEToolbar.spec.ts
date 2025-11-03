import { expect, describe } from '@jest/globals';
import { createServiceFactory, mockProvider, SpectatorService } from '@ngneat/spectator/jest';
import { patchState, signalStore, withState } from '@ngrx/signals';
import { of } from 'rxjs';

import { ActivatedRoute, Router } from '@angular/router';

import { DotPropertiesService } from '@dotcms/data-access';
import { DEFAULT_VARIANT_ID, DEFAULT_VARIANT_NAME } from '@dotcms/dotcms-models';
import { UVE_MODE } from '@dotcms/types';
import { getRunningExperimentMock, mockDotDevices } from '@dotcms/utils-testing';

import { withUVEToolbar } from './withUVEToolbar';

import { DotPageApiService } from '../../../../services/dot-page-api.service';
import { DEFAULT_PERSONA, PERSONA_KEY } from '../../../../shared/consts';
import { UVE_STATUS } from '../../../../shared/enums';
import { MOCK_RESPONSE_HEADLESS, mockCurrentUser } from '../../../../shared/mocks';
import { Orientation, UVEState } from '../../../models';

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
    pageAPIResponse: MOCK_RESPONSE_HEADLESS,
    currentUser: mockCurrentUser,
    experiment: null,
    errorCode: null,
    pageParams,
    status: UVE_STATUS.LOADED,
    isTraditionalPage: false,
    canEditPage: true,
    pageIsLocked: true,
    isClientReady: false,
    viewParams: {
        orientation: undefined,
        seo: undefined,
        device: undefined
    }
};

export const uveStoreMock = signalStore(
    { protectedState: false },
    withState<UVEState>(initialState),
    withUVEToolbar()
);

describe('withEditor', () => {
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
                        pageAPIResponse: {
                            ...MOCK_RESPONSE_HEADLESS,
                            viewAs: {
                                ...MOCK_RESPONSE_HEADLESS.viewAs,
                                variantId: variantID
                            }
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
                        pageAPIResponse: {
                            ...MOCK_RESPONSE_HEADLESS,
                            viewAs: {
                                ...MOCK_RESPONSE_HEADLESS.viewAs,
                                variantId: variantID
                            }
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
                        pageAPIResponse: {
                            ...MOCK_RESPONSE_HEADLESS,
                            viewAs: {
                                ...MOCK_RESPONSE_HEADLESS.viewAs,
                                variantId: variantID
                            }
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
                    pageAPIResponse: {
                        ...store.pageAPIResponse(),
                        viewAs: {
                            ...store.pageAPIResponse().viewAs,
                            variantId: DEFAULT_VARIANT_ID
                        }
                    }
                });
                expect(store.$showWorkflowsActions()).toBe(true);
            });

            it('should return false when not in preview mode and is not default variant', () => {
                patchState(store, {
                    pageAPIResponse: {
                        ...store.pageAPIResponse(),
                        viewAs: {
                            ...store.pageAPIResponse().viewAs,
                            variantId: 'some-other-variant'
                        }
                    }
                });
                expect(store.$showWorkflowsActions()).toBe(false);
            });
        });

        describe('$unlockButton', () => {
            it('should be null if the page is not locked', () => {
                patchState(store, {
                    pageAPIResponse: {
                        ...store.pageAPIResponse(),
                        page: {
                            ...store.pageAPIResponse().page,
                            locked: false
                        }
                    }
                });

                expect(store.$unlockButton()).toBe(null);
            });

            it('should be null if the page is locked by the current user', () => {
                patchState(store, {
                    pageAPIResponse: {
                        ...store.pageAPIResponse(),
                        page: {
                            ...store.pageAPIResponse().page,
                            locked: true,
                            lockedBy: mockCurrentUser.userId
                        }
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
                    pageAPIResponse: {
                        ...store.pageAPIResponse(),
                        page: {
                            ...store.pageAPIResponse().page,
                            locked: true,
                            lockedBy: '123',
                            lockedByName: 'John Doe'
                        }
                    },
                    pageParams: {
                        ...store.pageParams(),
                        mode: UVE_MODE.PREVIEW
                    }
                });

                expect(store.$unlockButton()).toEqual({
                    inode: store.pageAPIResponse().page.inode,
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
                    pageAPIResponse: {
                        ...store.pageAPIResponse(),
                        page: {
                            ...store.pageAPIResponse().page,
                            locked: true,
                            lockedBy: '123',
                            lockedByName: 'John Doe'
                        }
                    },
                    pageParams: {
                        ...store.pageParams(),
                        mode: UVE_MODE.LIVE
                    }
                });

                expect(store.$unlockButton()).toEqual({
                    inode: store.pageAPIResponse().page.inode,
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
                    pageAPIResponse: {
                        ...MOCK_RESPONSE_HEADLESS,
                        page: {
                            ...MOCK_RESPONSE_HEADLESS.page,
                            locked: true,
                            canLock: true,
                            lockedByName: 'John Doe',
                            lockedBy: '456'
                        }
                    },
                    pageParams: {
                        ...store.pageParams(),
                        mode: UVE_MODE.EDIT
                    },
                    status: UVE_STATUS.LOADED
                });
                expect(store.$unlockButton()).toEqual({
                    inode: store.pageAPIResponse().page.inode,
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
                    pageAPIResponse: {
                        ...MOCK_RESPONSE_HEADLESS,
                        page: {
                            ...MOCK_RESPONSE_HEADLESS.page,
                            locked: true,
                            lockedBy: '123',
                            lockedByName: 'John Doe',
                            canLock: false
                        }
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
                    inode: store.pageAPIResponse().page.inode,
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
                expect(store.device()).toBe(iphone);
                expect(store.orientation()).toBe(Orientation.LANDSCAPE); // This mock is on landscape, because the width is greater than the height

                expect(store.viewParams()).toEqual({
                    device: iphone.inode,
                    orientation: Orientation.LANDSCAPE,
                    seo: null
                });
            });

            it('should set the device and orientation', () => {
                const iphone = mockDotDevices[0];

                store.setDevice(iphone, Orientation.PORTRAIT);
                expect(store.device()).toBe(iphone);
                expect(store.orientation()).toBe(Orientation.PORTRAIT);

                expect(store.viewParams()).toEqual({
                    device: iphone.inode,
                    orientation: Orientation.PORTRAIT,
                    seo: null
                });
            });
        });

        describe('setOrientation', () => {
            it('should set the orientation and the view params', () => {
                store.setOrientation(Orientation.PORTRAIT);
                expect(store.orientation()).toBe(Orientation.PORTRAIT);

                expect(store.viewParams()).toEqual({
                    device: store.viewParams().device,
                    orientation: Orientation.PORTRAIT,
                    seo: undefined
                });
            });

            describe('clearDeviceAndSocialMedia', () => {
                // We have to extend this test because we have to test the social media
                it('should clear the device and social media', () => {
                    store.setDevice(mockDotDevices[0]);

                    store.clearDeviceAndSocialMedia();

                    expect(store.device()).toBe(null);
                    expect(store.socialMedia()).toBe(null);
                    expect(store.isEditState()).toBe(true);
                    expect(store.orientation()).toBe(null);
                    expect(store.viewParams()).toEqual({
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

                expect(store.socialMedia()).toBe('seo');
                expect(store.device()).toBe(null);
                expect(store.orientation()).toBe(null);

                expect(store.viewParams()).toEqual({
                    device: null,
                    orientation: null,
                    seo: 'seo'
                });
            });
        });
    });
});
