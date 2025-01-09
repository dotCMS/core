import { expect, describe } from '@jest/globals';
import { createServiceFactory, mockProvider, SpectatorService } from '@ngneat/spectator/jest';
import { patchState, signalStore, withState } from '@ngrx/signals';
import { of } from 'rxjs';

import { ActivatedRoute, Router } from '@angular/router';

import { UVE_MODE } from '@dotcms/client';
import { CurrentUser } from '@dotcms/dotcms-js';
import { DEFAULT_VARIANT_ID, DEFAULT_VARIANT_NAME } from '@dotcms/dotcms-models';
import { getRunningExperimentMock, mockDotDevices } from '@dotcms/utils-testing';

import { withUVEToolbar } from './withUVEToolbar';

import { DotPageApiService } from '../../../../services/dot-page-api.service';
import { DEFAULT_PERSONA } from '../../../../shared/consts';
import { UVE_STATUS } from '../../../../shared/enums';
import { MOCK_RESPONSE_HEADLESS } from '../../../../shared/mocks';
import { Orientation, UVEState } from '../../../models';

const pageParams = {
    url: 'test-url',
    language_id: '1',
    'com.dotmarketing.persona.id': 'dot:persona',
    variantName: 'DEFAULT',
    clientHost: 'http://localhost:3000'
};

const initialState: UVEState = {
    isEnterprise: true,
    languages: [],
    pageAPIResponse: MOCK_RESPONSE_HEADLESS,
    currentUser: null,
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

const currentUser: CurrentUser = {
    email: 'test@example.com',
    givenName: 'Test',
    loginAs: false,
    roleId: 'role123',
    surname: 'User',
    userId: 'user123'
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

        describe('infoDisplayProps', () => {
            it('should return null', () => {
                expect(store.$infoDisplayProps()).toBe(null);
            });

            describe('socialMedia', () => {
                it('should text for current social media', () => {
                    store.setSEO('facebook');

                    expect(store.$infoDisplayProps()).toEqual({
                        icon: 'pi pi-facebook',
                        id: 'socialMedia',
                        info: {
                            message: 'Viewing <b>facebook</b> social media preview',
                            args: []
                        },
                        actionIcon: 'pi pi-times'
                    });
                });
            });

            describe('variant', () => {
                it('should show have text for variant', () => {
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
                        experiment: currentExperiment
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
            });

            describe('edit permissions', () => {
                it('should show label and icon for no permissions', () => {
                    patchState(store, {
                        canEditPage: false
                    });

                    expect(store.$infoDisplayProps()).toEqual({
                        icon: 'pi pi-exclamation-circle warning',
                        id: 'no-permission',
                        info: {
                            message: 'editema.dont.have.edit.permission',
                            args: []
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
                        currentUser
                    });
                    expect(store.$infoDisplayProps()).toEqual({
                        icon: 'pi pi-lock',
                        id: 'locked',
                        info: {
                            message: 'editpage.locked-by',
                            args: ['John Doe']
                        }
                    });
                });

                it("should show a different message for that can't be locked", () => {
                    patchState(store, {
                        pageAPIResponse: {
                            ...MOCK_RESPONSE_HEADLESS,
                            page: {
                                ...MOCK_RESPONSE_HEADLESS.page,
                                locked: true,
                                canLock: false,
                                lockedByName: currentUser.givenName,
                                lockedBy: currentUser.userId
                            }
                        },
                        currentUser: {
                            ...currentUser,
                            userId: '123'
                        }
                    });

                    expect(store.$infoDisplayProps()).toEqual({
                        icon: 'pi pi-lock',
                        id: 'locked',
                        info: {
                            message: 'editpage.locked-contact-with',
                            args: ['Test']
                        }
                    });
                });

                it('should be null when locked by the same user', () => {
                    patchState(store, {
                        pageAPIResponse: {
                            ...MOCK_RESPONSE_HEADLESS,
                            page: {
                                ...MOCK_RESPONSE_HEADLESS.page,
                                locked: true,
                                canLock: true,
                                lockedByName: currentUser.givenName,
                                lockedBy: currentUser.userId
                            }
                        },
                        currentUser
                    });

                    expect(store.$infoDisplayProps()).toBe(null);
                });
            });

            describe('$showWorkflowsActions', () => {
                it('should return false when in preview mode', () => {
                    patchState(store, {
                        pageParams: {
                            ...store.pageParams(),
                            editorMode: UVE_MODE.PREVIEW
                        }
                    });
                    expect(store.$showWorkflowsActions()).toBe(false);
                });

                it('should return true when not in preview mode and is default variant', () => {
                    patchState(store, {
                        pageParams: {
                            ...store.pageParams(),
                            editorMode: UVE_MODE.EDIT
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
                    seo: undefined
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
                    seo: undefined
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
                        device: undefined,
                        orientation: undefined,
                        seo: undefined
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
