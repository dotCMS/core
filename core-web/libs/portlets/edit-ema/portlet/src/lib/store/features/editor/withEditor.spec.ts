import { describe, expect } from '@jest/globals';
import { SpyObject } from '@ngneat/spectator';
import { createServiceFactory, mockProvider, SpectatorService } from '@ngneat/spectator/jest';
import { patchState, signalStore, withState } from '@ngrx/signals';
import { of } from 'rxjs';

import { ActivatedRoute, Router } from '@angular/router';

import { UVE_MODE } from '@dotcms/client';
import { CurrentUser } from '@dotcms/dotcms-js';
import { DEFAULT_VARIANT_ID, DEFAULT_VARIANT_NAME, DotCMSContentlet } from '@dotcms/dotcms-models';
import { getRunningExperimentMock, mockDotDevices, seoOGTagsMock } from '@dotcms/utils-testing';

import { withEditor } from './withEditor';

import { DotPageApiParams, DotPageApiService } from '../../../services/dot-page-api.service';
import { BASE_IFRAME_MEASURE_UNIT, DEFAULT_PERSONA } from '../../../shared/consts';
import { EDITOR_STATE, UVE_STATUS } from '../../../shared/enums';
import {
    ACTION_MOCK,
    ACTION_PAYLOAD_MOCK,
    EMA_DRAG_ITEM_CONTENTLET_MOCK,
    getBoundsMock,
    MOCK_CONTENTLET_AREA,
    MOCK_RESPONSE_HEADLESS,
    MOCK_RESPONSE_VTL
} from '../../../shared/mocks';
import { DotDeviceWithIcon } from '../../../shared/models';
import { getPersonalization, mapContainerStructureToArrayOfContainers } from '../../../utils';
import { UVEState } from '../../models';

const mockCurrentUser: CurrentUser = {
    email: 'admin@dotcms.com',
    givenName: 'Admin',
    loginAs: true,
    roleId: 'e7d4e34e-5127-45fc-8123-d48b62d510e3',
    surname: 'User',
    userId: 'dotcms.org.1'
};
const mockOtherUser: CurrentUser = {
    email: 'admin2@dotcms.com',
    givenName: 'Admin2',
    loginAs: true,
    roleId: '73ec980e-d74f-4cec-a4d0-e319061e20b9',
    surname: 'User',
    userId: 'dotcms.org.2808'
};

const emptyParams = {} as DotPageApiParams;

const initialState: UVEState = {
    isEnterprise: true,
    languages: [],
    pageAPIResponse: MOCK_RESPONSE_HEADLESS,
    currentUser: null,
    experiment: null,
    errorCode: null,
    pageParams: {
        ...emptyParams,
        url: 'test-url',
        language_id: '1',
        'com.dotmarketing.persona.id': 'dot:persona',
        variantName: 'DEFAULT',
        clientHost: 'http://localhost:3000'
    },
    status: UVE_STATUS.LOADED,
    isTraditionalPage: false,
    canEditPage: true,
    pageIsLocked: true,
    isClientReady: false
};

export const uveStoreMock = signalStore(
    { protectedState: false },
    withState<UVEState>(initialState),
    withEditor()
);

describe('withEditor', () => {
    let spectator: SpectatorService<InstanceType<typeof uveStoreMock>>;
    let dotPageApiService: SpyObject<DotPageApiService>;
    let store: InstanceType<typeof uveStoreMock>;

    const createService = createServiceFactory({
        service: uveStoreMock,
        providers: [
            mockProvider(Router),
            mockProvider(ActivatedRoute),
            mockProvider(Router),
            mockProvider(ActivatedRoute),
            {
                provide: DotPageApiService,
                useValue: {
                    get() {
                        return of({});
                    },
                    getClientPage() {
                        return of({});
                    },
                    save: jest.fn()
                }
            }
        ]
    });

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;
        dotPageApiService = spectator.inject(DotPageApiService);
        patchState(store, initialState);
    });

    describe('withEditorToolbar', () => {
        describe('withComputed', () => {
            describe('$toolbarProps', () => {
                it('should return the base info', () => {
                    expect(store.$toolbarProps()).toEqual({
                        apiUrl: '/api/v1/page/json/test-url?language_id=1&com.dotmarketing.persona.id=dot%3Apersona&variantName=DEFAULT&clientHost=http%3A%2F%2Flocalhost%3A3000',
                        bookmarksUrl: '/test-url?host_id=123-xyz-567-xxl&language_id=1',
                        copyUrl:
                            'http://localhost:3000/test-url?language_id=1&com.dotmarketing.persona.id=dot%3Apersona&variantName=DEFAULT&host_id=123-xyz-567-xxl',
                        currentLanguage: MOCK_RESPONSE_HEADLESS.viewAs.language,
                        deviceSelector: {
                            apiLink:
                                'http://localhost:3000/api/v1/page/json/test-url?language_id=1&com.dotmarketing.persona.id=dot%3Apersona&variantName=DEFAULT&clientHost=http%3A%2F%2Flocalhost%3A3000',
                            hideSocialMedia: true
                        },
                        personaSelector: {
                            pageId: MOCK_RESPONSE_HEADLESS.page.identifier,
                            value: MOCK_RESPONSE_HEADLESS.viewAs.persona ?? DEFAULT_PERSONA
                        },
                        runningExperiment: null,
                        isDefaultVariant: true,
                        showInfoDisplay: false,
                        unlockButton: null,
                        urlContentMap: null,
                        workflowActionsInode: MOCK_RESPONSE_HEADLESS.page.inode
                    });
                });

                describe('urlContentMap', () => {
                    it('should return the urlContentMap if the state is edit', () => {
                        patchState(store, {
                            pageAPIResponse: {
                                ...MOCK_RESPONSE_HEADLESS,
                                urlContentMap: {
                                    title: 'Title',
                                    inode: '123',
                                    contentType: 'test'
                                } as unknown as DotCMSContentlet
                            }
                        });

                        expect(store.$toolbarProps().urlContentMap).toEqual({
                            title: 'Title',
                            inode: '123',
                            contentType: 'test'
                        });
                    });

                    it('should not return the urlContentMap if the state is not edit', () => {
                        patchState(store, { isEditState: false });
                        patchState(store, {
                            pageAPIResponse: {
                                ...MOCK_RESPONSE_HEADLESS,
                                urlContentMap: {
                                    title: 'Title',
                                    inode: '123',
                                    contentType: 'test'
                                } as unknown as DotCMSContentlet
                            }
                        });

                        expect(store.$toolbarProps().urlContentMap).toEqual(null);
                    });
                });

                describe('runningExperiment', () => {
                    it('should have a runningExperiment if the experiment is running', () => {
                        patchState(store, { experiment: getRunningExperimentMock() });

                        expect(store.$toolbarProps().runningExperiment).toEqual(
                            getRunningExperimentMock()
                        );
                    });
                });

                describe('workflowActionsInode', () => {
                    it("should not have an workflowActionsInode if the user can't edit the page", () => {
                        patchState(store, { canEditPage: false });

                        expect(store.$toolbarProps().workflowActionsInode).toBe(null);
                    });
                });

                describe('unlockButton', () => {
                    it('should display unlockButton if the page is locked by another user and the current user can lock the page', () => {
                        patchState(store, {
                            pageAPIResponse: {
                                ...MOCK_RESPONSE_HEADLESS,
                                page: {
                                    ...MOCK_RESPONSE_HEADLESS.page,
                                    locked: true,
                                    lockedBy: mockOtherUser.userId,
                                    canLock: true
                                }
                            },
                            currentUser: mockCurrentUser,
                            status: UVE_STATUS.LOADED
                        });

                        expect(store.$toolbarProps().unlockButton).toEqual({
                            inode: '123-i',
                            loading: false
                        });
                    });

                    it('should not display unlockButton if the page is locked by the current user', () => {
                        patchState(store, {
                            pageAPIResponse: {
                                ...MOCK_RESPONSE_HEADLESS,
                                page: {
                                    ...MOCK_RESPONSE_HEADLESS.page,
                                    locked: true,
                                    lockedBy: mockCurrentUser.userId,
                                    canLock: true
                                }
                            },
                            currentUser: mockCurrentUser
                        });

                        expect(store.$toolbarProps().unlockButton).toBeNull();
                    });

                    it('should not display unlockButton if the page is not locked', () => {
                        patchState(store, {
                            pageAPIResponse: {
                                ...MOCK_RESPONSE_HEADLESS,
                                page: {
                                    ...MOCK_RESPONSE_HEADLESS.page,
                                    locked: false,
                                    canLock: true
                                }
                            },
                            currentUser: mockCurrentUser
                        });

                        expect(store.$toolbarProps().unlockButton).toBeNull();
                    });

                    it('should not display unlockButton if the user cannot lock the page', () => {
                        patchState(store, {
                            pageAPIResponse: {
                                ...MOCK_RESPONSE_HEADLESS,
                                page: {
                                    ...MOCK_RESPONSE_HEADLESS.page,
                                    locked: true,
                                    canLock: false
                                }
                            },
                            currentUser: mockCurrentUser
                        });

                        expect(store.$toolbarProps().unlockButton).toBeNull();
                    });
                });

                describe('shouldShowInfoDisplay', () => {
                    it("should have shouldShowInfoDisplay as true if the user can't edit the page", () => {
                        patchState(store, { canEditPage: false });

                        expect(store.$toolbarProps().showInfoDisplay).toBe(true);
                    });

                    it('should have shouldShowInfoDisplay as true if the device is set', () => {
                        patchState(store, { device: mockDotDevices[0] });

                        expect(store.$toolbarProps().showInfoDisplay).toBe(true);
                    });

                    it('should have shouldShowInfoDisplay as true if the socialMedia is set', () => {
                        patchState(store, { socialMedia: 'facebook' });

                        expect(store.$toolbarProps().showInfoDisplay).toBe(true);
                    });

                    it('should have shouldShowInfoDisplay as true if the page is a variant different from default', () => {
                        patchState(store, {
                            pageAPIResponse: {
                                ...MOCK_RESPONSE_HEADLESS,
                                viewAs: {
                                    ...MOCK_RESPONSE_HEADLESS.viewAs,
                                    variantId: 'test'
                                }
                            }
                        });

                        expect(store.$toolbarProps().showInfoDisplay).toBe(true);
                    });
                    it('should have shouldShowInfoDisplay as true if the page is locked by another user', () => {
                        patchState(store, {
                            pageAPIResponse: {
                                ...MOCK_RESPONSE_HEADLESS,
                                page: {
                                    ...MOCK_RESPONSE_HEADLESS.page,
                                    locked: true,
                                    lockedBy: mockOtherUser.userId
                                }
                            },
                            currentUser: mockCurrentUser
                        });

                        expect(store.$toolbarProps().showInfoDisplay).toBe(true);
                    });

                    it('should have shouldShowInfoDisplay as false if the page is locked by the current user and other conditions are not met', () => {
                        patchState(store, {
                            pageAPIResponse: {
                                ...MOCK_RESPONSE_HEADLESS,
                                page: {
                                    ...MOCK_RESPONSE_HEADLESS.page,
                                    locked: true,
                                    lockedBy: mockCurrentUser.userId
                                }
                            },
                            currentUser: mockCurrentUser,
                            canEditPage: true,
                            device: null,
                            socialMedia: null
                        });

                        expect(store.$toolbarProps().showInfoDisplay).toBe(false);
                    });
                });

                describe('isDefaultVariant', () => {
                    it('should have isDefaultVariant as true if the page variant is default', () => {
                        patchState(store, {
                            pageAPIResponse: {
                                ...MOCK_RESPONSE_HEADLESS,
                                viewAs: {
                                    ...MOCK_RESPONSE_HEADLESS.viewAs,
                                    variantId: DEFAULT_VARIANT_ID
                                }
                            }
                        });

                        expect(store.$toolbarProps().isDefaultVariant).toBe(true);
                    });
                    it('should have isDefaultVariant as false if the page is a variant different from default', () => {
                        patchState(store, {
                            pageAPIResponse: {
                                ...MOCK_RESPONSE_HEADLESS,
                                viewAs: {
                                    ...MOCK_RESPONSE_HEADLESS.viewAs,
                                    variantId: 'test'
                                }
                            }
                        });

                        expect(store.$toolbarProps().isDefaultVariant).toBe(false);
                    });
                });
            });

            describe('$infoDisplayOptions', () => {
                it('should be null in regular conditions', () => {
                    patchState(store, { canEditPage: true, pageAPIResponse: null });
                    expect(store.$infoDisplayOptions()).toBe(null);
                });

                it('should return info for device', () => {
                    const device = mockDotDevices[0] as DotDeviceWithIcon;

                    patchState(store, { device });

                    expect(store.$infoDisplayOptions()).toEqual({
                        icon: device.icon,
                        info: {
                            message: 'iphone 200 x 100',
                            args: []
                        },
                        id: 'device',
                        actionIcon: 'pi pi-times'
                    });
                });

                it('should return info for socialMedia', () => {
                    patchState(store, { socialMedia: 'Facebook' });

                    expect(store.$infoDisplayOptions()).toEqual({
                        icon: 'pi pi-facebook',
                        info: {
                            message: 'Viewing <b>Facebook</b> social media preview',
                            args: []
                        },
                        id: 'socialMedia',
                        actionIcon: 'pi pi-times'
                    });
                });

                it('should return info when visiting a variant and can edit', () => {
                    const currentExperiment = getRunningExperimentMock();

                    const variantID = currentExperiment.trafficProportion.variants.find(
                        (variant) => variant.name !== DEFAULT_VARIANT_NAME
                    ).id;

                    patchState(store, {
                        canEditPage: true,
                        pageAPIResponse: {
                            ...MOCK_RESPONSE_HEADLESS,
                            viewAs: {
                                ...MOCK_RESPONSE_HEADLESS.viewAs,
                                variantId: variantID
                            }
                        },
                        experiment: currentExperiment
                    });

                    expect(store.$infoDisplayOptions()).toEqual({
                        icon: 'pi pi-file-edit',
                        info: {
                            message: 'editpage.editing.variant',
                            args: ['Variant A']
                        },
                        id: 'variant',
                        actionIcon: 'pi pi-arrow-left'
                    });
                });

                it('should return info when visiting a variant and can not edit', () => {
                    const currentExperiment = getRunningExperimentMock();

                    const variantID = currentExperiment.trafficProportion.variants.find(
                        (variant) => variant.name !== DEFAULT_VARIANT_NAME
                    ).id;

                    patchState(store, {
                        pageAPIResponse: {
                            ...MOCK_RESPONSE_HEADLESS,
                            page: {
                                ...MOCK_RESPONSE_HEADLESS.page
                            },
                            viewAs: {
                                ...MOCK_RESPONSE_HEADLESS.viewAs,
                                variantId: variantID
                            }
                        },
                        experiment: currentExperiment,
                        canEditPage: false
                    });

                    expect(store.$infoDisplayOptions()).toEqual({
                        icon: 'pi pi-file-edit',
                        info: {
                            message: 'editpage.viewing.variant',
                            args: ['Variant A']
                        },
                        id: 'variant',
                        actionIcon: 'pi pi-arrow-left'
                    });
                });

                it('should return info when the page is locked and can lock', () => {
                    patchState(store, {
                        pageAPIResponse: {
                            ...MOCK_RESPONSE_HEADLESS,
                            page: {
                                ...MOCK_RESPONSE_HEADLESS.page,
                                locked: true,
                                canLock: true,
                                lockedByName: 'John Doe'
                            }
                        }
                    });

                    expect(store.$infoDisplayOptions()).toEqual({
                        icon: 'pi pi-lock',
                        info: {
                            message: 'editpage.locked-by',
                            args: ['John Doe']
                        },
                        id: 'locked'
                    });
                });

                it('should return info when the page is locked and cannot lock', () => {
                    patchState(store, {
                        pageAPIResponse: {
                            ...MOCK_RESPONSE_HEADLESS,
                            page: {
                                ...MOCK_RESPONSE_HEADLESS.page,
                                locked: true,
                                canLock: false,
                                lockedByName: 'John Doe'
                            }
                        }
                    });

                    expect(store.$infoDisplayOptions()).toEqual({
                        icon: 'pi pi-lock',
                        info: {
                            message: 'editpage.locked-contact-with',
                            args: ['John Doe']
                        },
                        id: 'locked'
                    });
                });

                it('should return info when you cannot edit the page', () => {
                    patchState(store, { canEditPage: false });

                    expect(store.$infoDisplayOptions()).toEqual({
                        icon: 'pi pi-exclamation-circle warning',
                        info: { message: 'editema.dont.have.edit.permission', args: [] },
                        id: 'no-permission'
                    });
                });
            });
        });

        describe('withMethods', () => {
            it('should set the device with setDevice', () => {
                const device = {
                    identifier: '123',
                    cssHeight: '120',
                    cssWidth: '120',
                    name: 'square',
                    inode: '1234',
                    icon: 'icon'
                };

                store.setDevice(device);

                expect(store.device()).toEqual(device);
                expect(store.isEditState()).toBe(false);
            });

            it('should set the socialMedia with setSocialMedia', () => {
                const socialMedia = 'facebook';

                store.setSocialMedia(socialMedia);

                expect(store.socialMedia()).toEqual(socialMedia);
                expect(store.isEditState()).toBe(false);
            });

            it('should reset the state with clearDeviceAndSocialMedia', () => {
                store.clearDeviceAndSocialMedia();

                expect(store.device()).toBe(null);
                expect(store.socialMedia()).toBe(null);
                expect(store.isEditState()).toBe(true);
            });
        });
    });

    describe('withUVEToolbar', () => {
        describe('withComputed', () => {
            describe('$toolbarProps', () => {
                it('should return the base info', () => {
                    expect(store.$uveToolbar()).toEqual({
                        editor: {
                            apiUrl: '/api/v1/page/json/test-url?language_id=1&com.dotmarketing.persona.id=dot%3Apersona&variantName=DEFAULT&clientHost=http%3A%2F%2Flocalhost%3A3000',
                            bookmarksUrl: '/test-url?host_id=123-xyz-567-xxl&language_id=1',
                            copyUrl:
                                'http://localhost:3000/test-url?language_id=1&com.dotmarketing.persona.id=dot%3Apersona&variantName=DEFAULT&host_id=123-xyz-567-xxl'
                        },
                        preview: null,
                        currentLanguage: MOCK_RESPONSE_HEADLESS.viewAs.language,
                        urlContentMap: null,
                        runningExperiment: null,
                        unlockButton: null,
                        showInfoDisplay: false
                    });
                });
            });
        });
    });

    describe('withSave', () => {
        describe('withMethods', () => {
            describe('savePage', () => {
                it('should perform a save and patch the state', () => {
                    const saveSpy = jest
                        .spyOn(dotPageApiService, 'save')
                        .mockImplementation(() => of({}));

                    // It's impossible to get a VTL when we are in Headless
                    // but I just want to check the state is being patched
                    const getClientPageSpy = jest
                        .spyOn(dotPageApiService, 'getClientPage')
                        .mockImplementation(() => of(MOCK_RESPONSE_VTL));

                    const payload = {
                        pageContainers: ACTION_PAYLOAD_MOCK.pageContainers,
                        pageId: MOCK_RESPONSE_HEADLESS.page.identifier,
                        params: store.pageParams()
                    };

                    store.savePage(ACTION_PAYLOAD_MOCK.pageContainers);

                    expect(saveSpy).toHaveBeenCalledWith(payload);

                    expect(getClientPageSpy).toHaveBeenCalledWith(
                        store.pageParams(),
                        store.clientRequestProps()
                    );

                    expect(store.status()).toBe(UVE_STATUS.LOADED);
                    expect(store.pageAPIResponse()).toEqual(MOCK_RESPONSE_VTL);
                });
            });
        });
    });

    describe('withComputed', () => {
        describe('$pageData', () => {
            it('should return the expected data', () => {
                expect(store.$pageData()).toEqual({
                    containers: mapContainerStructureToArrayOfContainers(
                        MOCK_RESPONSE_HEADLESS.containers
                    ),
                    id: MOCK_RESPONSE_HEADLESS.page.identifier,
                    personalization: getPersonalization(MOCK_RESPONSE_HEADLESS.viewAs.persona),
                    languageId: MOCK_RESPONSE_HEADLESS.viewAs.language.id,
                    personaTag: MOCK_RESPONSE_HEADLESS.viewAs.persona.keyTag
                });
            });
        });

        describe('$reloadEditorContent', () => {
            it('should return the expected data for Headless', () => {
                patchState(store, {
                    pageAPIResponse: MOCK_RESPONSE_HEADLESS,
                    isTraditionalPage: false
                });

                expect(store.$reloadEditorContent()).toEqual({
                    code: MOCK_RESPONSE_HEADLESS.page.rendered,
                    isTraditionalPage: false,
                    enableInlineEdit: true,
                    isClientReady: false
                });
            });
            it('should return the expected data for VTL', () => {
                patchState(store, {
                    pageAPIResponse: MOCK_RESPONSE_VTL,
                    isTraditionalPage: true,
                    isClientReady: true
                });
                expect(store.$reloadEditorContent()).toEqual({
                    code: MOCK_RESPONSE_VTL.page.rendered,
                    isTraditionalPage: true,
                    enableInlineEdit: true,
                    isClientReady: true
                });
            });
        });

        describe('$editorIsInDraggingState', () => {
            it("should return the editor's dragging state", () => {
                expect(store.$editorIsInDraggingState()).toBe(false);
            });

            it("should return the editor's dragging state after a change", () => {
                // This will trigger a change in the dragging state
                store.setEditorDragItem(EMA_DRAG_ITEM_CONTENTLET_MOCK);

                expect(store.$editorIsInDraggingState()).toBe(true);
            });
        });

        describe('$iframeURL', () => {
            it("should return the iframe's URL", () => {
                expect(store.$iframeURL()).toBe(
                    'http://localhost:3000/test-url?language_id=1&com.dotmarketing.persona.id=dot%3Apersona&variantName=DEFAULT&clientHost=http%3A%2F%2Flocalhost%3A3000'
                );
            });

            it('should be an instance of String in src when the page is traditional', () => {
                patchState(store, {
                    pageAPIResponse: MOCK_RESPONSE_VTL,
                    isTraditionalPage: true
                });

                expect(store.$iframeURL()).toContain('about:blank');
            });

            it('should contain the right url when the page is a vanity url  ', () => {
                patchState(store, {
                    pageAPIResponse: {
                        ...MOCK_RESPONSE_HEADLESS,
                        vanityUrl: {
                            ...MOCK_RESPONSE_HEADLESS.vanityUrl,
                            url: 'first'
                        }
                    }
                });

                expect(store.$iframeURL()).toBe(
                    'http://localhost:3000/first?language_id=1&com.dotmarketing.persona.id=dot%3Apersona&variantName=DEFAULT&clientHost=http%3A%2F%2Flocalhost%3A3000'
                );
            });
        });

        describe('$editorProps', () => {
            it('should return the expected data on init', () => {
                expect(store.$editorProps()).toEqual({
                    showDialogs: true,
                    showEditorContent: true,
                    showBlockEditorSidebar: true,
                    iframe: {
                        opacity: '0.5',
                        pointerEvents: 'auto',
                        wrapper: null
                    },
                    progressBar: true,
                    contentletTools: null,
                    dropzone: null,
                    palette: {
                        variantId: DEFAULT_VARIANT_ID,
                        languageId: MOCK_RESPONSE_HEADLESS.viewAs.language.id,
                        containers: MOCK_RESPONSE_HEADLESS.containers
                    },
                    seoResults: null
                });
            });

            it('should set iframe opacity to 1 when client is Ready', () => {
                store.setIsClientReady(true);

                expect(store.$editorProps().iframe.opacity).toBe('1');
            });

            it('should not have opacity or progressBar in preview mode', () => {
                patchState(store, {
                    pageParams: { ...emptyParams, editorMode: UVE_MODE.PREVIEW }
                });

                expect(store.$editorProps().iframe.opacity).toBe('1');
                expect(store.$editorProps().progressBar).toBe(false);
            });

            describe('showDialogs', () => {
                it('should have the value of false when we cannot edit the page', () => {
                    patchState(store, { canEditPage: false });

                    expect(store.$editorProps().showDialogs).toBe(false);
                });

                it('should have the value of false when we are not on edit state', () => {
                    patchState(store, { isEditState: false });

                    expect(store.$editorProps().showDialogs).toBe(false);
                });
            });

            describe('showEditorContent', () => {
                it('should have showEditorContent as true when there is no socialMedia', () => {
                    expect(store.$editorProps().showEditorContent).toBe(true);
                });
            });

            describe('iframe', () => {
                it('should have an opacity of 0.5 when loading', () => {
                    patchState(store, { status: UVE_STATUS.LOADING });

                    expect(store.$editorProps().iframe.opacity).toBe('0.5');
                });

                it('should have pointerEvents as none when dragging', () => {
                    patchState(store, { state: EDITOR_STATE.DRAGGING });

                    expect(store.$editorProps().iframe.pointerEvents).toBe('none');
                });

                it('should have pointerEvents as none when scroll-drag', () => {
                    patchState(store, { state: EDITOR_STATE.SCROLL_DRAG });

                    expect(store.$editorProps().iframe.pointerEvents).toBe('none');
                });

                it('should have a wrapper when a device is present', () => {
                    const device = mockDotDevices[0] as DotDeviceWithIcon;

                    patchState(store, { device });

                    expect(store.$editorProps().iframe.wrapper).toEqual({
                        width: device.cssWidth + BASE_IFRAME_MEASURE_UNIT,
                        height: device.cssHeight + BASE_IFRAME_MEASURE_UNIT
                    });
                });
            });

            describe('progressBar', () => {
                it('should have progressBar as true when the status is loading', () => {
                    patchState(store, { status: UVE_STATUS.LOADING });

                    expect(store.$editorProps().progressBar).toBe(true);
                });

                it('should have progressBar as true when the status is loaded but client is not ready', () => {
                    patchState(store, { status: UVE_STATUS.LOADED, isClientReady: false });

                    expect(store.$editorProps().progressBar).toBe(true);
                });

                it('should have progressBar as false when the status is loaded and client is ready', () => {
                    patchState(store, { status: UVE_STATUS.LOADED, isClientReady: true });

                    expect(store.$editorProps().progressBar).toBe(false);
                });
            });

            describe('contentletTools', () => {
                it('should have contentletTools when contentletArea are present, can edit the page, is in edit state and not scrolling', () => {
                    patchState(store, {
                        isEditState: true,
                        canEditPage: true,
                        contentletArea: MOCK_CONTENTLET_AREA,
                        state: EDITOR_STATE.IDLE
                    });

                    expect(store.$editorProps().contentletTools).toEqual({
                        isEnterprise: true,
                        contentletArea: MOCK_CONTENTLET_AREA,
                        hide: false
                    });
                });

                it('should have hide as true when dragging', () => {
                    patchState(store, {
                        isEditState: true,
                        canEditPage: true,
                        contentletArea: MOCK_CONTENTLET_AREA,
                        state: EDITOR_STATE.DRAGGING
                    });

                    expect(store.$editorProps().contentletTools).toEqual({
                        isEnterprise: true,
                        contentletArea: MOCK_CONTENTLET_AREA,
                        hide: true
                    });
                });

                it('should be null when scrolling', () => {
                    patchState(store, {
                        isEditState: true,
                        canEditPage: true,
                        contentletArea: MOCK_CONTENTLET_AREA,
                        state: EDITOR_STATE.SCROLLING
                    });

                    expect(store.$editorProps().contentletTools).toEqual(null);
                });

                it("should not have contentletTools when the page can't be edited", () => {
                    patchState(store, {
                        isEditState: true,
                        canEditPage: false,
                        contentletArea: MOCK_CONTENTLET_AREA,
                        state: EDITOR_STATE.IDLE
                    });

                    expect(store.$editorProps().contentletTools).toBe(null);
                });

                it('should not have contentletTools when the contentletArea is not present', () => {
                    patchState(store, {
                        isEditState: true,
                        canEditPage: true,
                        state: EDITOR_STATE.IDLE
                    });

                    expect(store.$editorProps().contentletTools).toBe(null);
                });

                it('should not have contentletTools when the we are not in edit state', () => {
                    patchState(store, {
                        isEditState: false,
                        canEditPage: true,
                        contentletArea: MOCK_CONTENTLET_AREA,
                        state: EDITOR_STATE.IDLE
                    });

                    expect(store.$editorProps().contentletTools).toBe(null);
                });

                it('should have contentletTools when the page can be edited and is in preview mode', () => {
                    patchState(store, {
                        isEditState: true,
                        canEditPage: true,
                        pageParams: {
                            ...emptyParams,
                            editorMode: UVE_MODE.PREVIEW
                        },
                        state: EDITOR_STATE.IDLE
                    });

                    expect(store.$editorProps().contentletTools).toEqual(null);
                });
            });
            describe('dropzone', () => {
                const bounds = getBoundsMock(ACTION_MOCK);

                it('should have dropzone when the state is dragging and the page can be edited', () => {
                    patchState(store, {
                        state: EDITOR_STATE.DRAGGING,
                        canEditPage: true,
                        dragItem: EMA_DRAG_ITEM_CONTENTLET_MOCK,
                        bounds
                    });

                    expect(store.$editorProps().dropzone).toEqual({
                        dragItem: EMA_DRAG_ITEM_CONTENTLET_MOCK,
                        bounds
                    });
                });

                it("should not have dropzone when the page can't be edited", () => {
                    patchState(store, {
                        state: EDITOR_STATE.DRAGGING,
                        canEditPage: false,
                        dragItem: EMA_DRAG_ITEM_CONTENTLET_MOCK,
                        bounds
                    });

                    expect(store.$editorProps().dropzone).toBe(null);
                });
            });

            describe('palette', () => {
                it('should be null if is not enterprise', () => {
                    patchState(store, { isEnterprise: false });

                    expect(store.$editorProps().palette).toBe(null);
                });

                it('should be null if canEditPage is false', () => {
                    patchState(store, { canEditPage: false });

                    expect(store.$editorProps().palette).toBe(null);
                });

                it('should be null if isEditState is false', () => {
                    patchState(store, { isEditState: false });

                    expect(store.$editorProps().palette).toBe(null);
                });
            });

            describe('seoResults', () => {
                it('should have the expected data when ogTags and socialMedia is present', () => {
                    patchState(store, {
                        ogTags: seoOGTagsMock,
                        socialMedia: 'facebook'
                    });

                    expect(store.$editorProps().seoResults).toEqual({
                        ogTags: seoOGTagsMock,
                        socialMedia: 'facebook'
                    });
                });

                it('should be null when ogTags is not present', () => {
                    patchState(store, {
                        socialMedia: 'facebook'
                    });

                    expect(store.$editorProps().seoResults).toBe(null);
                });

                it('should be null when socialMedia is not present', () => {
                    patchState(store, {
                        ogTags: seoOGTagsMock
                    });

                    expect(store.$editorProps().seoResults).toBe(null);
                });
            });
        });
    });

    describe('withMethods', () => {
        describe('updateEditorScrollState', () => {
            it("should update the editor's scroll state and remove bounds when there is no drag item", () => {
                store.updateEditorScrollState();

                expect(store.state()).toEqual(EDITOR_STATE.SCROLLING);
                expect(store.bounds()).toEqual([]);
            });

            it("should update the editor's scroll drag state and remove bounds when there is drag item", () => {
                store.setEditorDragItem(EMA_DRAG_ITEM_CONTENTLET_MOCK);
                store.setEditorBounds(getBoundsMock(ACTION_MOCK));

                store.updateEditorScrollState();

                expect(store.state()).toEqual(EDITOR_STATE.SCROLL_DRAG);
                expect(store.bounds()).toEqual([]);
            });

            it('should set the contentletArea to null when we are scrolling', () => {
                store.setEditorState(EDITOR_STATE.SCROLLING);

                store.updateEditorScrollState();

                expect(store.contentletArea()).toBe(null);
            });
        });

        describe('updateEditorOnScrollEnd', () => {
            it("should update the editor's drag state when there is no drag item", () => {
                store.updateEditorOnScrollEnd();

                expect(store.state()).toEqual(EDITOR_STATE.IDLE);
            });

            it("should update the editor's drag state when there is drag item", () => {
                store.setEditorDragItem(EMA_DRAG_ITEM_CONTENTLET_MOCK);

                store.updateEditorOnScrollEnd();

                expect(store.state()).toEqual(EDITOR_STATE.DRAGGING);
            });
        });

        describe('updateEditorScrollDragState', () => {
            it('should update the store correctly', () => {
                store.updateEditorScrollDragState();

                expect(store.state()).toEqual(EDITOR_STATE.SCROLL_DRAG);
                expect(store.bounds()).toEqual([]);
            });
        });

        describe('setEditorState', () => {
            it('should update the state correctly', () => {
                store.setEditorState(EDITOR_STATE.SCROLLING);

                expect(store.state()).toEqual(EDITOR_STATE.SCROLLING);
            });
        });

        describe('setEditorDragItem', () => {
            it('should update the store correctly', () => {
                store.setEditorDragItem(EMA_DRAG_ITEM_CONTENTLET_MOCK);

                expect(store.dragItem()).toEqual(EMA_DRAG_ITEM_CONTENTLET_MOCK);
                expect(store.state()).toEqual(EDITOR_STATE.DRAGGING);
            });
        });

        describe('setEditorContentletArea', () => {
            it("should update the store's contentlet area", () => {
                store.setEditorContentletArea(MOCK_CONTENTLET_AREA);

                expect(store.contentletArea()).toEqual(MOCK_CONTENTLET_AREA);
                expect(store.state()).toEqual(EDITOR_STATE.IDLE);
            });

            it('should not update contentletArea if it is the same', () => {
                store.setEditorContentletArea(MOCK_CONTENTLET_AREA);

                // We can have contentletArea and state at the same time we are inline editing
                store.setEditorState(EDITOR_STATE.INLINE_EDITING);

                store.setEditorContentletArea(MOCK_CONTENTLET_AREA);

                expect(store.contentletArea()).toEqual(MOCK_CONTENTLET_AREA);
                // State should not change
                expect(store.state()).toEqual(EDITOR_STATE.INLINE_EDITING);
            });
        });

        describe('setEditorBounds', () => {
            const bounds = getBoundsMock(ACTION_MOCK);

            it('should update the store correcly', () => {
                store.setEditorBounds(bounds);

                expect(store.bounds()).toEqual(bounds);
            });
        });

        describe('resetEditorProperties', () => {
            it('should reset the editor props corretcly', () => {
                store.setEditorDragItem(EMA_DRAG_ITEM_CONTENTLET_MOCK);
                store.setEditorState(EDITOR_STATE.SCROLLING);
                store.setEditorContentletArea(MOCK_CONTENTLET_AREA);
                store.setEditorBounds(getBoundsMock(ACTION_MOCK));

                store.resetEditorProperties();

                expect(store.dragItem()).toBe(null);
                expect(store.state()).toEqual(EDITOR_STATE.IDLE);
                expect(store.contentletArea()).toBe(null);
                expect(store.bounds()).toEqual([]);
            });
        });
        describe('getPageSavePayload', () => {
            it("should return the page's save payload", () => {
                expect(store.getPageSavePayload(ACTION_PAYLOAD_MOCK)).toEqual({
                    container: {
                        acceptTypes: 'test',
                        contentletsId: [],
                        identifier: 'container-identifier-123',
                        maxContentlets: 1,
                        uuid: 'uuid-123',
                        variantId: '123'
                    },
                    contentlet: {
                        contentType: 'test',
                        identifier: 'contentlet-identifier-123',
                        inode: 'contentlet-inode-123',
                        onNumberOfPages: 1,
                        title: 'Hello World'
                    },
                    language_id: '1',
                    pageContainers: [
                        {
                            contentletsId: ['123', '456'],
                            identifier: '5363c6c6-5ba0-4946-b7af-cf875188ac2e',
                            uuid: '123'
                        },
                        {
                            contentletsId: ['123'],
                            identifier: '5363c6c6-5ba0-4946-b7af-cf875188ac2e',
                            uuid: '456'
                        },
                        {
                            contentletsId: ['123', '456'],
                            identifier: '/container/path',
                            uuid: '123'
                        },
                        {
                            contentletsId: ['123'],
                            identifier: '/container/path',
                            uuid: '456'
                        }
                    ],
                    pageId: '123',
                    personaTag: 'dot:persona',
                    position: 'after'
                });
            });
        });

        describe('getCurrentTreeNode', () => {
            it('should return the current TreeNode', () => {
                const { container, contentlet } = ACTION_PAYLOAD_MOCK;

                expect(store.getCurrentTreeNode(container, contentlet)).toEqual({
                    containerId: 'container-identifier-123',
                    contentId: 'contentlet-identifier-123',
                    pageId: '123',
                    personalization: 'dot:persona:dot:persona',
                    relationType: 'uuid-123',
                    treeOrder: '-1',
                    variantId: '123'
                });
            });
        });

        describe('setOgTags', () => {
            it('should set the ogTags correctly', () => {
                const ogTags = {
                    title: 'Title',
                    description: 'Description',
                    image: 'Image',
                    type: 'Type',
                    url: 'URL'
                };

                store.setOgTags(ogTags);

                expect(store.ogTags()).toEqual(ogTags);
            });
        });
    });
});
