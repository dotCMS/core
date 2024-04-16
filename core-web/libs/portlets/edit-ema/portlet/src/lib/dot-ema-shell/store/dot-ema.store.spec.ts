import { describe, expect } from '@jest/globals';
import { createServiceFactory, SpectatorService, SpyObject } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { MessageService } from 'primeng/api';

import {
    DotContentletLockerService,
    DotExperimentsService,
    DotLicenseService,
    DotMessageService
} from '@dotcms/data-access';
import { LoginService } from '@dotcms/dotcms-js';
import {
    getDraftExperimentMock,
    getRunningExperimentMock,
    getScheduleExperimentMock,
    mockDotContainers,
    mockDotLayout,
    MockDotMessageService,
    mockDotTemplate,
    mockSites
} from '@dotcms/utils-testing';

import { EditEmaStore } from './dot-ema.store';

import { DotPageApiResponse, DotPageApiService } from '../../services/dot-page-api.service';
import { DEFAULT_PERSONA, MOCK_RESPONSE_HEADLESS } from '../../shared/consts';
import { EDITOR_MODE, EDITOR_STATE } from '../../shared/enums';
import { ActionPayload } from '../../shared/models';

const MOCK_RESPONSE_VTL: DotPageApiResponse = {
    page: {
        pageURI: 'test-url',
        title: 'Test Page',
        identifier: '123',
        inode: '123-i',
        canEdit: true,
        canRead: true,
        rendered: '<html><body><h1>Hello, World!</h1></body></html>',
        contentType: 'htmlpageasset',
        canLock: true,
        locked: false,
        lockedBy: '',
        lockedByName: ''
    },
    viewAs: {
        language: {
            id: 1,
            language: 'English',
            countryCode: 'US',
            languageCode: '1',
            country: 'United States'
        },

        persona: {
            ...DEFAULT_PERSONA
        }
    },
    site: mockSites[0],
    layout: mockDotLayout(),
    template: mockDotTemplate(),
    containers: mockDotContainers()
};

describe('EditEmaStore', () => {
    describe('EditEmaStore Headless', () => {
        let spectator: SpectatorService<EditEmaStore>;
        let dotPageApiService: SpyObject<DotPageApiService>;

        const createService = createServiceFactory({
            service: EditEmaStore,
            providers: [
                MessageService,
                {
                    provide: DotPageApiService,
                    useValue: {
                        get() {
                            return of({});
                        },
                        save: jest.fn(),
                        getFormIndetifier: jest.fn()
                    }
                },
                {
                    provide: DotLicenseService,
                    useValue: {
                        isEnterprise: () => of(true)
                    }
                },
                {
                    provide: DotMessageService,
                    useValue: new MockDotMessageService({})
                },
                {
                    provide: DotExperimentsService,
                    useValue: {
                        getById(experimentId: string) {
                            if (experimentId == 'i-have-a-running-experiment') {
                                return of(getRunningExperimentMock());
                            } else if (experimentId == 'i-have-a-scheduled-experiment') {
                                return of(getScheduleExperimentMock());
                            } else if (experimentId) return of(getDraftExperimentMock());

                            return of(null);
                        }
                    }
                },
                {
                    provide: DotContentletLockerService,
                    useValue: {
                        unlock: (_inode: string) => of({})
                    }
                },
                {
                    provide: LoginService,
                    useValue: {
                        getCurrentUser: () => of({})
                    }
                }
            ]
        });

        beforeEach(() => {
            spectator = createService();

            dotPageApiService = spectator.inject(DotPageApiService);
            jest.spyOn(dotPageApiService, 'get').mockImplementation(({ url }) => {
                return of({
                    ...MOCK_RESPONSE_HEADLESS,
                    page: {
                        ...MOCK_RESPONSE_HEADLESS.page,
                        pageURI: url
                    }
                });
            });

            spectator.service.load({
                clientHost: 'http://localhost:3000',
                language_id: '1',
                url: 'test-url',
                'com.dotmarketing.persona.id': '123'
            });
        });

        describe('selectors', () => {
            it('should return editorState', (done) => {
                const dotPageApiService = spectator.inject(DotPageApiService);

                jest.spyOn(dotPageApiService, 'get').mockReturnValue(
                    of({
                        ...MOCK_RESPONSE_HEADLESS,
                        page: {
                            ...MOCK_RESPONSE_HEADLESS.page,
                            pageURI: 'test-url'
                        }
                    })
                );

                spectator.service.load({
                    clientHost: 'http://localhost:3000',
                    language_id: '1',
                    url: 'test-url',
                    'com.dotmarketing.persona.id': '123'
                });

                spectator.service.editorState$.subscribe((state) => {
                    expect(state).toEqual({
                        bounds: [],
                        contentletArea: null,
                        clientHost: 'http://localhost:3000',
                        editor: MOCK_RESPONSE_HEADLESS,
                        currentExperiment: null,
                        apiURL: 'http://localhost/api/v1/page/json/test-url?language_id=1&com.dotmarketing.persona.id=modes.persona.no.persona&variantName=DEFAULT&mode=EDIT_MODE',
                        iframeURL:
                            'http://localhost:3000/test-url?language_id=1&com.dotmarketing.persona.id=modes.persona.no.persona&variantName=DEFAULT&mode=EDIT_MODE',
                        isEnterpriseLicense: true,
                        favoritePageURL: '/test-url?host_id=123-xyz-567-xxl&language_id=1',
                        state: EDITOR_STATE.IDLE,
                        editorData: {
                            mode: EDITOR_MODE.EDIT,
                            canEditPage: true,
                            canEditVariant: true,
                            page: {
                                canLock: true,
                                isLocked: false,
                                lockedByUser: ''
                            },
                            variantId: undefined
                        }
                    });
                    done();
                });
            });

            it('should return editorState with canEditPage setted to false', (done) => {
                const headlessResponseWithoutEditPermission: DotPageApiResponse = {
                    ...MOCK_RESPONSE_HEADLESS,
                    page: {
                        ...MOCK_RESPONSE_HEADLESS.page,
                        canEdit: false
                    }
                };

                const dotPageApiService = spectator.inject(DotPageApiService);

                jest.spyOn(dotPageApiService, 'get').mockReturnValue(
                    of(headlessResponseWithoutEditPermission)
                );

                spectator.service.load({
                    clientHost: 'http://localhost:3000',
                    language_id: '1',
                    url: 'test-url',
                    'com.dotmarketing.persona.id': '123'
                });

                spectator.service.editorState$.subscribe((state) => {
                    expect(state).toEqual({
                        bounds: [],
                        contentletArea: null,
                        clientHost: 'http://localhost:3000',
                        editor: headlessResponseWithoutEditPermission,
                        currentExperiment: null,
                        apiURL: 'http://localhost/api/v1/page/json/test-url?language_id=1&com.dotmarketing.persona.id=modes.persona.no.persona&variantName=DEFAULT&mode=EDIT_MODE',
                        iframeURL:
                            'http://localhost:3000/test-url?language_id=1&com.dotmarketing.persona.id=modes.persona.no.persona&variantName=DEFAULT&mode=EDIT_MODE',
                        isEnterpriseLicense: true,
                        favoritePageURL: '/test-url?host_id=123-xyz-567-xxl&language_id=1',
                        state: EDITOR_STATE.IDLE,
                        editorData: {
                            mode: EDITOR_MODE.EDIT,
                            canEditPage: false,
                            canEditVariant: true,
                            page: {
                                canLock: true,
                                isLocked: false,
                                lockedByUser: ''
                            },
                            variantId: undefined
                        }
                    });
                    done();
                });
            });

            it('should return cannot edit variant for a page with a running experiment', (done) => {
                const currentExperiment = getRunningExperimentMock();

                spectator.service.load({
                    clientHost: 'http://localhost:3000',
                    language_id: '1',
                    url: 'test-url',
                    'com.dotmarketing.persona.id': '123',
                    experimentId: 'i-have-a-running-experiment',
                    variantName: currentExperiment.trafficProportion.variants[1].id
                });

                spectator.service.editorState$.subscribe((state) => {
                    expect(state).toEqual({
                        bounds: [],
                        contentletArea: null,
                        clientHost: 'http://localhost:3000',
                        editor: MOCK_RESPONSE_HEADLESS,
                        currentExperiment,
                        apiURL: 'http://localhost/api/v1/page/json/test-url?language_id=1&com.dotmarketing.persona.id=modes.persona.no.persona&variantName=111&mode=EDIT_MODE',
                        iframeURL:
                            'http://localhost:3000/test-url?language_id=1&com.dotmarketing.persona.id=modes.persona.no.persona&variantName=111&mode=EDIT_MODE',
                        isEnterpriseLicense: true,
                        favoritePageURL: '/test-url?host_id=123-xyz-567-xxl&language_id=1',
                        state: EDITOR_STATE.IDLE,
                        editorData: {
                            mode: EDITOR_MODE.PREVIEW_VARIANT,
                            canEditPage: true,
                            canEditVariant: false,
                            variantId: '111',
                            page: {
                                canLock: true,
                                isLocked: false,
                                lockedByUser: ''
                            }
                        }
                    });
                    done();
                });
            });

            it('should return cannot edit variant for a page with a scheduled experiment', (done) => {
                const currentExperiment = getScheduleExperimentMock();

                spectator.service.load({
                    clientHost: 'http://localhost:3000',
                    language_id: '1',
                    url: 'test-url',
                    'com.dotmarketing.persona.id': '123',
                    experimentId: 'i-have-a-scheduled-experiment',
                    variantName: currentExperiment.trafficProportion.variants[1].id
                });

                spectator.service.editorState$.subscribe((state) => {
                    expect(state).toEqual({
                        bounds: [],
                        contentletArea: null,
                        clientHost: 'http://localhost:3000',
                        editor: MOCK_RESPONSE_HEADLESS,
                        currentExperiment,
                        apiURL: 'http://localhost/api/v1/page/json/test-url?language_id=1&com.dotmarketing.persona.id=modes.persona.no.persona&variantName=222&mode=EDIT_MODE',
                        iframeURL:
                            'http://localhost:3000/test-url?language_id=1&com.dotmarketing.persona.id=modes.persona.no.persona&variantName=222&mode=EDIT_MODE',
                        isEnterpriseLicense: true,
                        favoritePageURL: '/test-url?host_id=123-xyz-567-xxl&language_id=1',
                        state: EDITOR_STATE.IDLE,
                        editorData: {
                            mode: EDITOR_MODE.PREVIEW_VARIANT,
                            canEditPage: true,
                            canEditVariant: false,
                            variantId: '222',
                            page: {
                                canLock: true,
                                isLocked: false,
                                lockedByUser: ''
                            }
                        }
                    });
                    done();
                });
            });

            it('should return can edit variant for a page with a draft experiment', (done) => {
                const currentExperiment = getDraftExperimentMock();

                spectator.service.load({
                    clientHost: 'http://localhost:3000',
                    language_id: '1',
                    url: 'test-url',
                    'com.dotmarketing.persona.id': '123',
                    experimentId: 'i-have-a-draft-experiment',
                    variantName: currentExperiment.trafficProportion.variants[1].id
                });

                spectator.service.editorState$.subscribe((state) => {
                    expect(state).toEqual({
                        bounds: [],
                        contentletArea: null,
                        clientHost: 'http://localhost:3000',
                        editor: MOCK_RESPONSE_HEADLESS,
                        currentExperiment,
                        apiURL: 'http://localhost/api/v1/page/json/test-url?language_id=1&com.dotmarketing.persona.id=modes.persona.no.persona&variantName=111&mode=EDIT_MODE',
                        iframeURL:
                            'http://localhost:3000/test-url?language_id=1&com.dotmarketing.persona.id=modes.persona.no.persona&variantName=111&mode=EDIT_MODE',
                        isEnterpriseLicense: true,
                        favoritePageURL: '/test-url?host_id=123-xyz-567-xxl&language_id=1',
                        state: EDITOR_STATE.IDLE,
                        editorData: {
                            mode: EDITOR_MODE.EDIT_VARIANT,
                            canEditPage: true,
                            canEditVariant: true,
                            variantId: '111',
                            page: {
                                canLock: true,
                                isLocked: false,
                                lockedByUser: ''
                            }
                        }
                    });
                    done();
                });
            });

            it('should return contentState', (done) => {
                spectator.service.contentState$.subscribe((state) => {
                    expect(state).toEqual({
                        state: EDITOR_STATE.IDLE,
                        code: undefined
                    });
                    done();
                });
            });

            it('should return layoutProperties', (done) => {
                const containersMapMock = {
                    '/default/': {
                        type: 'containers',
                        identifier: '5363c6c6-5ba0-4946-b7af-cf875188ac2e',
                        name: 'Medium Column (md-1)',
                        categoryId: '9ab97328-e72f-4d7e-8be6-232f53218a93',
                        source: 'DB',
                        parentPermissionable: {
                            hostname: 'demo.dotcms.com'
                        }
                    },
                    '/banner/': {
                        type: 'containers',
                        identifier: '56bd55ea-b04b-480d-9e37-5d6f9217dcc3',
                        name: 'Large Column (lg-1)',
                        categoryId: 'dde0b865-6cea-4ff0-8582-85e5974cf94f',
                        source: 'FILE',
                        path: '/container/path',
                        parentPermissionable: {
                            hostname: 'demo.dotcms.com'
                        }
                    }
                };
                spectator.service.layoutProperties$.subscribe((state) => {
                    expect(state).toEqual({
                        layout: mockDotLayout(),
                        themeId: mockDotTemplate().theme,
                        pageId: '123',
                        containersMap: containersMapMock,
                        template: {
                            identifier: '111',
                            themeId: undefined
                        }
                    });
                    done();
                });
            });
        });

        describe('updaters', () => {
            it('should update the editorState', (done) => {
                spectator.service.updateEditorState(EDITOR_STATE.IDLE);

                spectator.service.editorState$.subscribe((state) => {
                    expect(state).toEqual({
                        bounds: [],
                        contentletArea: null,
                        clientHost: 'http://localhost:3000',
                        editor: MOCK_RESPONSE_HEADLESS,
                        currentExperiment: null,
                        apiURL: 'http://localhost/api/v1/page/json/test-url?language_id=1&com.dotmarketing.persona.id=modes.persona.no.persona&variantName=DEFAULT&mode=EDIT_MODE',
                        iframeURL:
                            'http://localhost:3000/test-url?language_id=1&com.dotmarketing.persona.id=modes.persona.no.persona&variantName=DEFAULT&mode=EDIT_MODE',
                        isEnterpriseLicense: true,
                        favoritePageURL: '/test-url?host_id=123-xyz-567-xxl&language_id=1',
                        state: EDITOR_STATE.IDLE,
                        editorData: {
                            variandId: undefined,
                            mode: EDITOR_MODE.EDIT,
                            canEditVariant: true,
                            canEditPage: true,
                            page: {
                                canLock: true,
                                isLocked: false,
                                lockedByUser: ''
                            }
                        }
                    });
                    done();
                });
            });
        });

        describe('effects', () => {
            it('should handle successful data loading', (done) => {
                const dotPageApiService = spectator.inject(DotPageApiService);

                jest.spyOn(dotPageApiService, 'get').mockReturnValue(of(MOCK_RESPONSE_HEADLESS));

                spectator.service.load({
                    clientHost: 'http://localhost:3000',
                    language_id: '1',
                    url: 'test-url',
                    'com.dotmarketing.persona.id': '123'
                });

                spectator.service.state$.subscribe((state) => {
                    expect(state as unknown).toEqual({
                        bounds: [],
                        contentletArea: null,
                        clientHost: 'http://localhost:3000',
                        editor: MOCK_RESPONSE_HEADLESS,
                        currentExperiment: null,
                        isEnterpriseLicense: true,
                        editorState: EDITOR_STATE.IDLE,
                        editorData: {
                            mode: EDITOR_MODE.EDIT,
                            canEditVariant: true,
                            canEditPage: true,
                            page: {
                                canLock: true,
                                isLocked: false,
                                lockedByUser: ''
                            }
                        }
                    });
                    done();
                });
            });

            it('should handle successful data reload', (done) => {
                const dotPageApiService = spectator.inject(DotPageApiService);
                const spyWhenReloaded = jest.fn();
                const spyGetPage = jest
                    .spyOn(dotPageApiService, 'get')
                    .mockReturnValue(of(MOCK_RESPONSE_HEADLESS));
                const params = {
                    language_id: '1',
                    url: 'test-url',
                    'com.dotmarketing.persona.id': '123'
                };

                spectator.service.reload({
                    params,
                    whenReloaded: spyWhenReloaded
                });

                spectator.service.state$.subscribe((state) => {
                    expect(state).toEqual({
                        bounds: [],
                        contentletArea: null,
                        clientHost: 'http://localhost:3000',
                        editor: MOCK_RESPONSE_HEADLESS,
                        isEnterpriseLicense: true,
                        currentExperiment: null,
                        editorState: EDITOR_STATE.IDLE,
                        editorData: {
                            mode: EDITOR_MODE.EDIT,
                            canEditPage: true,
                            canEditVariant: true,
                            page: {
                                canLock: true,
                                isLocked: false,
                                lockedByUser: ''
                            }
                        }
                    });
                    expect(spyGetPage).toHaveBeenCalledWith(params);
                    expect(spyWhenReloaded).toHaveBeenCalled();
                    done();
                });
            });

            it("should call save method from dotPageApiService when 'save' action is dispatched", () => {
                const dotPageApiService = spectator.inject(DotPageApiService);
                // eslint-disable-next-line @typescript-eslint/no-explicit-any
                const mockResponse: any = {
                    page: {
                        title: 'Test Page'
                    }
                };
                jest.spyOn(dotPageApiService, 'get').mockReturnValue(of(mockResponse));

                spectator.service.load({
                    clientHost: 'http://localhost:3000',
                    language_id: '1',
                    url: 'test-url',
                    'com.dotmarketing.persona.id': '123'
                });

                spectator.service.savePage({
                    pageContainers: [],
                    pageId: '789'
                });

                expect(dotPageApiService.save).toHaveBeenCalledWith({
                    pageContainers: [],
                    pageId: '789'
                });
            });

            it("should call get method from dotPageApiService when 'save' action is dispatched", () => {
                const dotPageApiService = spectator.inject(DotPageApiService);

                jest.spyOn(dotPageApiService, 'save').mockReturnValue(of({}));
                // eslint-disable-next-line @typescript-eslint/no-explicit-any
                jest.spyOn(dotPageApiService, 'get').mockReturnValue(of({} as any));

                spectator.service.savePage({
                    pageContainers: [],
                    pageId: '789',
                    params: {
                        language_id: '2',
                        url: 'test-url',
                        'com.dotmarketing.persona.id': '456'
                    },
                    whenSaved: () => {
                        /** */
                    }
                });

                // This get called twice, once for the load in the before each and once for the save
                expect(dotPageApiService.get).toHaveBeenCalledWith({
                    language_id: '2',
                    url: 'test-url',
                    'com.dotmarketing.persona.id': '456'
                });
            });

            it('should add form to page and save', () => {
                const payload: ActionPayload = {
                    pageId: 'page-identifier-123',
                    language_id: '1',
                    container: {
                        identifier: 'container-identifier-123',
                        uuid: '123',
                        acceptTypes: 'test',
                        maxContentlets: 1,
                        contentletsId: ['existing-contentlet-123'],
                        variantId: '1'
                    },
                    pageContainers: [
                        {
                            identifier: 'container-identifier-123',
                            uuid: '123',
                            contentletsId: ['existing-contentlet-123']
                        }
                    ],
                    contentlet: {
                        identifier: 'existing-contentlet-123',
                        inode: 'existing-contentlet-inode-456',
                        title: 'Hello World',
                        contentType: 'test'
                    }
                };
                const dotPageApiService = spectator.inject(DotPageApiService);
                jest.spyOn(dotPageApiService, 'save').mockReturnValue(of({}));
                jest.spyOn(dotPageApiService, 'getFormIndetifier').mockReturnValue(
                    of('form-identifier-123')
                );

                spectator.service.load({
                    clientHost: 'http://localhost:3000',
                    language_id: '1',
                    url: 'test-url',
                    'com.dotmarketing.persona.id': '123'
                });
                spectator.service.saveFormToPage({
                    payload,
                    formId: 'form-identifier-789',
                    params: {
                        'com.dotmarketing.persona.id': '123',
                        url: 'test-url',
                        language_id: '1'
                    },
                    whenSaved: () => {
                        /** */
                    }
                });

                expect(dotPageApiService.getFormIndetifier).toHaveBeenCalledWith(
                    payload.container.identifier,
                    'form-identifier-789'
                );
                expect(dotPageApiService.save).toHaveBeenCalledWith({
                    pageContainers: [
                        {
                            contentletsId: ['existing-contentlet-123', 'form-identifier-123'],
                            identifier: 'container-identifier-123',
                            personaTag: undefined,
                            uuid: '123'
                        }
                    ],
                    pageId: 'page-identifier-123',
                    params: {
                        'com.dotmarketing.persona.id': '123',
                        url: 'test-url',
                        language_id: '1'
                    }
                });
            });

            it('should add form to page, save and perform a get afterwards', () => {
                const payload: ActionPayload = {
                    pageId: 'page-identifier-123',
                    language_id: '1',
                    container: {
                        identifier: 'container-identifier-123',
                        uuid: '123',
                        acceptTypes: 'test',
                        maxContentlets: 1,
                        contentletsId: ['existing-contentlet-123'],
                        variantId: '123'
                    },
                    pageContainers: [
                        {
                            identifier: 'container-identifier-123',
                            uuid: '123',
                            contentletsId: ['existing-contentlet-123']
                        }
                    ],
                    contentlet: {
                        identifier: 'existing-contentlet-123',
                        inode: 'existing-contentlet-inode-456',
                        title: 'Hello World',
                        contentType: 'test'
                    }
                };
                const dotPageApiService = spectator.inject(DotPageApiService);
                jest.spyOn(dotPageApiService, 'save').mockReturnValue(of({}));
                jest.spyOn(dotPageApiService, 'getFormIndetifier').mockReturnValue(
                    of('form-identifier-123')
                );

                spectator.service.load({
                    clientHost: 'http://localhost:3000',
                    language_id: '1',
                    url: 'test-url',
                    'com.dotmarketing.persona.id': '123'
                });
                spectator.service.saveFormToPage({
                    payload,
                    formId: 'form-identifier-789',
                    params: {
                        language_id: '2',
                        url: 'test-url',
                        'com.dotmarketing.persona.id': '456'
                    },
                    whenSaved: () => {
                        /* */
                    }
                });

                expect(dotPageApiService.getFormIndetifier).toHaveBeenCalledWith(
                    payload.container.identifier,
                    'form-identifier-789'
                );
                expect(dotPageApiService.get).toHaveBeenCalledWith({
                    language_id: '2',
                    url: 'test-url',
                    'com.dotmarketing.persona.id': '456'
                });
            });

            it('should not add form to page when the form is dupe and triggers a message', () => {
                const messageService = spectator.inject(MessageService);

                const addMessageSpy = jest.spyOn(messageService, 'add');

                const payload: ActionPayload = {
                    pageId: 'page-identifier-123',
                    language_id: '1',
                    container: {
                        identifier: 'container-identifier-123',
                        uuid: '123',
                        acceptTypes: 'test',
                        maxContentlets: 1,
                        contentletsId: ['existing-contentlet-123', 'form-identifier-123'],
                        variantId: '1'
                    },
                    pageContainers: [
                        {
                            identifier: 'container-identifier-123',
                            uuid: '123',
                            contentletsId: ['existing-contentlet-123', 'form-identifier-123']
                        }
                    ],
                    contentlet: {
                        identifier: 'existing-contentlet-123',
                        inode: 'existing-contentlet-inode-456',
                        title: 'Hello World',
                        contentType: 'test'
                    }
                };
                const dotPageApiService = spectator.inject(DotPageApiService);
                jest.spyOn(dotPageApiService, 'save').mockReturnValue(of({}));
                jest.spyOn(dotPageApiService, 'getFormIndetifier').mockReturnValue(
                    of('form-identifier-123')
                );

                spectator.service.load({
                    clientHost: 'http://localhost:3000',
                    language_id: '1',
                    url: 'test-url',
                    'com.dotmarketing.persona.id': '123'
                });
                spectator.service.saveFormToPage({
                    payload,
                    formId: 'form-identifier-789',
                    params: {
                        'com.dotmarketing.persona.id': '123',
                        url: 'test-url',
                        language_id: '1'
                    },
                    whenSaved: () => {
                        //
                    }
                });

                expect(addMessageSpy).toHaveBeenCalledWith({
                    severity: 'info',
                    summary: 'editpage.content.add.already.title',
                    detail: 'editpage.content.add.already.message',
                    life: 2000
                });
            });
        });
    });

    describe('EditEmaStore VTL', () => {
        let spectator: SpectatorService<EditEmaStore>;
        let dotPageApiService: SpyObject<DotPageApiService>;

        const createService = createServiceFactory({
            service: EditEmaStore,
            mocks: [DotPageApiService],
            providers: [
                MessageService,
                {
                    provide: DotLicenseService,
                    useValue: {
                        isEnterprise: () => of(true)
                    }
                },
                {
                    provide: DotMessageService,
                    useValue: new MockDotMessageService({})
                },
                {
                    provide: DotExperimentsService,
                    useValue: {
                        getById(experimentId: string) {
                            if (experimentId == 'i-have-a-running-experiment') {
                                return of(getRunningExperimentMock());
                            } else if (experimentId == 'i-have-a-scheduled-experiment') {
                                return of(getScheduleExperimentMock());
                            } else if (experimentId) return of(getDraftExperimentMock());

                            return of(null);
                        }
                    }
                },
                {
                    provide: DotContentletLockerService,
                    useValue: {
                        unlock: (_inode: string) => of({})
                    }
                },
                {
                    provide: LoginService,
                    useValue: {
                        getCurrentUser: () => of({})
                    }
                }
            ]
        });

        beforeEach(() => {
            spectator = createService();

            dotPageApiService = spectator.inject(DotPageApiService);
            dotPageApiService.get.mockImplementation(({ url }) => {
                return of({
                    ...MOCK_RESPONSE_VTL,
                    page: {
                        ...MOCK_RESPONSE_VTL.page,
                        pageURI: url,
                        rendered: '<html><body><h1>Hello, World!</h1></body></html>'
                    }
                });
            });

            jest.spyOn(dotPageApiService, 'save').mockReturnValue(of({}));

            spectator.service.load({
                language_id: '1',
                url: 'test-url',
                'com.dotmarketing.persona.id': '123'
            });
        });

        describe('selectors', () => {
            it('should return page rendered', (done) => {
                spectator.service.pageRendered$.subscribe((rendered) => {
                    expect(rendered).toEqual('<html><body><h1>Hello, World!</h1></body></html>');
                    done();
                });
            });

            it('should return editorState', (done) => {
                spectator.service.editorState$.subscribe((state) => {
                    expect(state).toEqual({
                        bounds: [],
                        contentletArea: null,
                        clientHost: undefined,
                        editor: MOCK_RESPONSE_VTL,
                        currentExperiment: null,
                        apiURL: 'http://localhost/api/v1/page/json/test-url?language_id=1&com.dotmarketing.persona.id=modes.persona.no.persona&variantName=DEFAULT&mode=EDIT_MODE',
                        iframeURL: '',
                        isEnterpriseLicense: true,
                        favoritePageURL: '/test-url?host_id=123-xyz-567-xxl&language_id=1',
                        state: EDITOR_STATE.IDLE,
                        editorData: {
                            mode: EDITOR_MODE.EDIT,
                            canEditPage: true,
                            canEditVariant: true,
                            page: {
                                canLock: true,
                                isLocked: false,
                                lockedByUser: ''
                            },
                            variantId: undefined
                        }
                    });
                    done();
                });
            });

            it('should return contentState', (done) => {
                spectator.service.contentState$.subscribe((state) => {
                    expect(state).toEqual({
                        state: EDITOR_STATE.IDLE,
                        code: '<html><body><h1>Hello, World!</h1></body></html>'
                    });
                    done();
                });
            });
        });

        describe('updaters', () => {
            it('should update the editorState', (done) => {
                spectator.service.updateEditorState(EDITOR_STATE.IDLE);

                spectator.service.editorState$.subscribe((state) => {
                    expect(state).toEqual({
                        bounds: [],
                        contentletArea: null,
                        clientHost: undefined,
                        editor: MOCK_RESPONSE_VTL,
                        apiURL: 'http://localhost/api/v1/page/json/test-url?language_id=1&com.dotmarketing.persona.id=modes.persona.no.persona&variantName=DEFAULT&mode=EDIT_MODE',
                        iframeURL: '',
                        isEnterpriseLicense: true,
                        favoritePageURL: '/test-url?host_id=123-xyz-567-xxl&language_id=1',
                        state: EDITOR_STATE.IDLE,
                        editorData: {
                            mode: EDITOR_MODE.EDIT,
                            canEditPage: true,
                            canEditVariant: true,
                            page: {
                                canLock: true,
                                isLocked: false,
                                lockedByUser: ''
                            },
                            variantId: undefined
                        },
                        currentExperiment: null
                    });
                    done();
                });
            });
        });

        describe('effects', () => {
            it('should handle successful data loading', (done) => {
                const dotPageApiService = spectator.inject(DotPageApiService);

                dotPageApiService.get.andReturn(of(MOCK_RESPONSE_VTL));

                spectator.service.load({
                    language_id: '1',
                    url: 'test-url',
                    'com.dotmarketing.persona.id': '123'
                });

                spectator.service.state$.subscribe((state) => {
                    expect(state as unknown).toEqual({
                        bounds: [],
                        contentletArea: null,
                        clientHost: undefined,
                        editor: MOCK_RESPONSE_VTL,
                        isEnterpriseLicense: true,
                        currentExperiment: null,
                        editorState: EDITOR_STATE.IDLE,
                        editorData: {
                            mode: EDITOR_MODE.EDIT,
                            canEditPage: true,
                            canEditVariant: true,
                            page: {
                                canLock: true,
                                isLocked: false,
                                lockedByUser: ''
                            },
                            variantId: undefined
                        }
                    });
                    done();
                });
            });

            it("should call save method from dotPageApiService when 'save' action is dispatched", () => {
                const dotPageApiService = spectator.inject(DotPageApiService);
                const mockResponse = {
                    page: {
                        title: 'Test Page'
                    }
                };
                dotPageApiService.get.andReturn(of(mockResponse));

                spectator.service.load({
                    language_id: '1',
                    url: 'test-url',
                    'com.dotmarketing.persona.id': '123'
                });

                spectator.service.savePage({
                    pageContainers: [],
                    pageId: '789'
                });

                expect(dotPageApiService.save).toHaveBeenCalledWith({
                    pageContainers: [],
                    pageId: '789'
                });
            });

            it('should add form to page and save', () => {
                const payload: ActionPayload = {
                    pageId: 'page-identifier-123',
                    language_id: '1',
                    container: {
                        identifier: 'container-identifier-123',
                        uuid: '123',
                        acceptTypes: 'test',
                        maxContentlets: 1,
                        contentletsId: ['existing-contentlet-123'],
                        variantId: '1'
                    },
                    pageContainers: [
                        {
                            identifier: 'container-identifier-123',
                            uuid: '123',
                            contentletsId: ['existing-contentlet-123']
                        }
                    ],
                    contentlet: {
                        identifier: 'existing-contentlet-123',
                        inode: 'existing-contentlet-inode-456',
                        title: 'Hello World',
                        contentType: 'test'
                    }
                };
                const dotPageApiService = spectator.inject(DotPageApiService);
                jest.spyOn(dotPageApiService, 'save').mockReturnValue(of({}));
                jest.spyOn(dotPageApiService, 'getFormIndetifier').mockReturnValue(
                    of('form-identifier-123')
                );

                spectator.service.load({
                    language_id: '1',
                    url: 'test-url',
                    'com.dotmarketing.persona.id': '123'
                });
                spectator.service.saveFormToPage({
                    payload,
                    formId: 'form-identifier-789',
                    params: {
                        'com.dotmarketing.persona.id': '123',
                        url: 'test-url',
                        language_id: '1'
                    },
                    whenSaved: () => {
                        //
                    }
                });

                expect(dotPageApiService.getFormIndetifier).toHaveBeenCalledWith(
                    payload.container.identifier,
                    'form-identifier-789'
                );
                expect(dotPageApiService.save).toHaveBeenCalledWith({
                    pageContainers: [
                        {
                            contentletsId: ['existing-contentlet-123', 'form-identifier-123'],
                            identifier: 'container-identifier-123',
                            personaTag: undefined,
                            uuid: '123'
                        }
                    ],
                    pageId: 'page-identifier-123',
                    params: {
                        'com.dotmarketing.persona.id': '123',
                        url: 'test-url',
                        language_id: '1'
                    }
                });
            });

            it('should call unlock page service', () => {
                const dotContentletLockerService = spectator.inject(DotContentletLockerService);
                const spyUnlock = jest.spyOn(dotContentletLockerService, 'unlock');
                const spyPatch = jest.spyOn(spectator.service, 'patchState');

                spectator.service.unlockPage('123');

                expect(spyUnlock).toHaveBeenCalledWith('123');
                expect(spyPatch).toHaveBeenCalled();
            });

            it('should not add form to page when the form is dupe and triggers a message', () => {
                const messageService = spectator.inject(MessageService);

                const addMessageSpy = jest.spyOn(messageService, 'add');

                const payload: ActionPayload = {
                    pageId: 'page-identifier-123',
                    language_id: '1',
                    container: {
                        identifier: 'container-identifier-123',
                        uuid: '123',
                        acceptTypes: 'test',
                        maxContentlets: 1,
                        contentletsId: ['existing-contentlet-123', 'form-identifier-123'],
                        variantId: '1'
                    },
                    pageContainers: [
                        {
                            identifier: 'container-identifier-123',
                            uuid: '123',
                            contentletsId: ['existing-contentlet-123', 'form-identifier-123']
                        }
                    ],
                    contentlet: {
                        identifier: 'existing-contentlet-123',
                        inode: 'existing-contentlet-inode-456',
                        title: 'Hello World',
                        contentType: 'test'
                    }
                };
                const dotPageApiService = spectator.inject(DotPageApiService);
                jest.spyOn(dotPageApiService, 'save').mockReturnValue(of({}));
                jest.spyOn(dotPageApiService, 'getFormIndetifier').mockReturnValue(
                    of('form-identifier-123')
                );

                spectator.service.load({
                    language_id: '1',
                    url: 'test-url',
                    'com.dotmarketing.persona.id': '123'
                });
                spectator.service.saveFormToPage({
                    payload,
                    formId: 'form-identifier-789',
                    params: {
                        'com.dotmarketing.persona.id': '123',
                        url: 'test-url',
                        language_id: '1'
                    },
                    whenSaved: () => {
                        //
                    }
                });

                expect(addMessageSpy).toHaveBeenCalledWith({
                    severity: 'info',
                    summary: 'editpage.content.add.already.title',
                    detail: 'editpage.content.add.already.message',
                    life: 2000
                });
            });
        });
    });
});
