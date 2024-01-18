import { describe, expect } from '@jest/globals';
import { createServiceFactory, SpectatorService, SpyObject } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { MessageService } from 'primeng/api';

import { DotLicenseService, DotMessageService } from '@dotcms/data-access';
import {
    MockDotMessageService,
    mockDotContainers,
    mockDotLayout,
    mockDotTemplate,
    mockSites
} from '@dotcms/utils-testing';

import { EditEmaStore } from './dot-ema.store';

import { DotActionUrlService } from '../../services/dot-action-url/dot-action-url.service';
import { DotPageApiResponse, DotPageApiService } from '../../services/dot-page-api.service';
import { DEFAULT_PERSONA, EDIT_CONTENTLET_URL } from '../../shared/consts';
import { EDITOR_STATE } from '../../shared/enums';
import { ActionPayload } from '../../shared/models';

const mockResponse: DotPageApiResponse = {
    page: {
        url: 'test-url',
        title: 'Test Page',
        identifier: '123',
        inode: '123-i',
        canEdit: true,
        canRead: true
    },
    viewAs: {
        language: {
            id: 1,
            language: 'English',
            countryCode: 'US',
            languageCode: 'En',
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
    let spectator: SpectatorService<EditEmaStore>;
    let dotPageApiService: SpyObject<DotPageApiService>;

    const createService = createServiceFactory({
        service: EditEmaStore,
        mocks: [DotPageApiService, DotActionUrlService],
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
            }
        ]
    });

    beforeEach(() => {
        spectator = createService();

        dotPageApiService = spectator.inject(DotPageApiService);
        dotPageApiService.get.mockImplementation(({ url }) => {
            return of({
                ...mockResponse,
                page: {
                    ...mockResponse.page,
                    url
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
            spectator.service.editorState$.subscribe((state) => {
                expect(state).toEqual({
                    clientHost: 'http://localhost:3000',
                    editor: mockResponse,
                    apiURL: 'http://localhost/api/v1/page/json/test-url?language_id=1&com.dotmarketing.persona.id=modes.persona.no.persona',
                    iframeURL: `http://localhost:3000/test-url?language_id=1&com.dotmarketing.persona.id=modes.persona.no.persona`,
                    isEnterpriseLicense: true,
                    favoritePageURL: '/test-url?host_id=123-xyz-567-xxl&language_id=1',
                    state: EDITOR_STATE.LOADING
                });
                done();
            });
        });
    });

    describe('updaters', () => {
        it('should update the editorState', () => {
            spectator.service.updateEditorState(EDITOR_STATE.LOADED);

            spectator.service.editorState$.subscribe((state) => {
                expect(state).toEqual({
                    clientHost: 'http://localhost:3000',
                    editor: mockResponse,
                    apiURL: 'http://localhost/api/v1/page/json/test-url?language_id=1&com.dotmarketing.persona.id=modes.persona.no.persona',
                    iframeURL: `http://localhost:3000/test-url?language_id=1&com.dotmarketing.persona.id=modes.persona.no.persona`,
                    isEnterpriseLicense: true,
                    favoritePageURL: '/test-url?host_id=123-xyz-567-xxl&language_id=1',
                    state: EDITOR_STATE.LOADED
                });
            });
        });

        it('should update editIframeLoading', (done) => {
            spectator.service.setDialogIframeLoading(true);

            spectator.service.state$.subscribe((state) => {
                expect(state).toEqual({
                    clientHost: 'http://localhost:3000',
                    editor: mockResponse,
                    dialogIframeURL: '',
                    dialogIframeLoading: true,
                    dialogHeader: '',
                    isEnterpriseLicense: true,
                    dialogType: null,
                    editorState: EDITOR_STATE.LOADING
                });
                done();
            });
        });

        it('should reset editIframe properties', (done) => {
            spectator.service.setDialogIframeLoading(true);

            spectator.service.resetDialog();

            spectator.service.state$.subscribe((state) => {
                expect(state).toEqual({
                    editor: mockResponse,
                    clientHost: 'http://localhost:3000',
                    dialogIframeURL: '',
                    dialogIframeLoading: false,
                    dialogHeader: '',
                    isEnterpriseLicense: true,
                    dialogType: null,
                    editorState: EDITOR_STATE.LOADING
                });
                done();
            });
        });

        it('should initialize editAction properties', (done) => {
            spectator.service.initActionEdit({
                inode: '123',
                title: 'test',
                type: 'content'
            });

            spectator.service.state$.subscribe((state) => {
                expect(state).toEqual({
                    editor: mockResponse,
                    clientHost: 'http://localhost:3000',
                    dialogIframeURL: EDIT_CONTENTLET_URL + '123',
                    dialogIframeLoading: true,
                    dialogHeader: 'test',
                    isEnterpriseLicense: true,
                    dialogType: 'content',
                    editorState: EDITOR_STATE.LOADING
                });
                done();
            });
        });

        it('should initialize addAction properties', (done) => {
            spectator.service.initActionAdd({
                containerId: '1234',
                acceptTypes: 'test',
                language_id: '1'
            });

            spectator.service.state$.subscribe((state) => {
                expect(state).toEqual({
                    editor: mockResponse,
                    clientHost: 'http://localhost:3000',
                    dialogIframeURL:
                        '/html/ng-contentlet-selector.jsp?ng=true&container_id=1234&add=test&language_id=1',
                    dialogIframeLoading: true,
                    dialogHeader: 'Search Content',
                    dialogType: 'content',
                    isEnterpriseLicense: true,
                    editorState: EDITOR_STATE.LOADING
                });
                done();
            });
        });

        it('should initialize createAction properties', (done) => {
            spectator.service.initActionCreate({
                contentType: 'test',
                url: 'some/really/long/url'
            });

            spectator.service.state$.subscribe((state) => {
                expect(state).toEqual({
                    editor: mockResponse,
                    clientHost: 'http://localhost:3000',
                    dialogIframeURL: 'some/really/long/url',
                    dialogIframeLoading: true,
                    dialogHeader: 'test',
                    dialogType: 'content',
                    isEnterpriseLicense: true,
                    editorState: EDITOR_STATE.LOADING
                });
                done();
            });
        });

        it('should update dialog state', (done) => {
            spectator.service.setDialogForCreateContent({
                url: 'some/really/long/url',
                name: 'Blog Posts'
            });

            spectator.service.state$.subscribe((state) => {
                expect(state.dialogHeader).toBe('Create Blog Posts');
                expect(state.dialogIframeLoading).toBe(true);
                expect(state.dialogIframeURL).toBe('some/really/long/url');
                expect(state.dialogType).toBe('content');
                done();
            });
        });
    });

    describe('effects', () => {
        it('should update state to show dialog for create content from palette', (done) => {
            const dotPageApiService = spectator.inject(DotPageApiService);
            const dotActionUrlService = spectator.inject(DotActionUrlService);

            dotPageApiService.get.andReturn(
                of({
                    page: {
                        title: 'Test Page',
                        identifier: '123',
                        url: 'page-url'
                    },
                    viewAs: {
                        language: {
                            id: 1,
                            language: '',
                            countryCode: '',
                            languageCode: '',
                            country: ''
                        }
                    }
                })
            );
            dotActionUrlService.getCreateContentletUrl.andReturn(
                of('https://demo.dotcms.com/jsp.jsp')
            );

            spectator.service.load({
                clientHost: 'http://localhost:3000',
                language_id: 'en',
                url: 'test-url',
                'com.dotmarketing.persona.id': '123'
            });

            spectator.service.createContentFromPalette({
                variable: 'blogPost',
                name: 'Blog'
            });

            spectator.service.state$.subscribe((state) => {
                expect(state.dialogHeader).toBe('Create Blog');
                expect(state.dialogIframeLoading).toBe(true);
                expect(state.dialogIframeURL).toBe('https://demo.dotcms.com/jsp.jsp');
                expect(state.dialogType).toBe('content');
                done();
            });

            expect(dotActionUrlService.getCreateContentletUrl).toHaveBeenCalledWith('blogPost');
        });

        it('should handle successful data loading', (done) => {
            const dotPageApiService = spectator.inject(DotPageApiService);

            dotPageApiService.get.andReturn(of(mockResponse));

            spectator.service.load({
                clientHost: 'http://localhost:3000',
                language_id: 'en',
                url: 'test-url',
                'com.dotmarketing.persona.id': '123'
            });

            spectator.service.state$.subscribe((state) => {
                expect(state as unknown).toEqual({
                    clientHost: 'http://localhost:3000',
                    editor: mockResponse,
                    dialogIframeURL: '',
                    dialogIframeLoading: false,
                    dialogHeader: '',
                    dialogType: null,
                    isEnterpriseLicense: true,
                    editorState: EDITOR_STATE.LOADING
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
                clientHost: 'http://localhost:3000',
                language_id: 'en',
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
                    contentletsId: ['existing-contentlet-123']
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
                    title: 'Hello World'
                }
            };
            const dotPageApiService = spectator.inject(DotPageApiService);
            dotPageApiService.save.andReturn(of({}));
            dotPageApiService.getFormIndetifier.andReturn(of('form-identifier-123'));

            spectator.service.load({
                clientHost: 'http://localhost:3000',
                language_id: 'en',
                url: 'test-url',
                'com.dotmarketing.persona.id': '123'
            });
            spectator.service.saveFormToPage({
                payload,
                formId: 'form-identifier-789',
                // eslint-disable-next-line @typescript-eslint/no-empty-function
                whenSaved: () => {}
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
                pageId: 'page-identifier-123'
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
                    contentletsId: ['existing-contentlet-123', 'form-identifier-123']
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
                    title: 'Hello World'
                }
            };
            const dotPageApiService = spectator.inject(DotPageApiService);
            dotPageApiService.save.andReturn(of({}));
            dotPageApiService.getFormIndetifier.andReturn(of('form-identifier-123'));

            spectator.service.load({
                clientHost: 'http://localhost:3000',
                language_id: 'en',
                url: 'test-url',
                'com.dotmarketing.persona.id': '123'
            });
            spectator.service.saveFormToPage({
                payload,
                formId: 'form-identifier-789',
                // eslint-disable-next-line @typescript-eslint/no-empty-function
                whenSaved: () => {}
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
