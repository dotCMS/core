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

import { DotPageApiResponse, DotPageApiService } from '../../services/dot-page-api.service';
import { DEFAULT_PERSONA } from '../../shared/consts';
import { EDITOR_STATE } from '../../shared/enums';
import { ActionPayload } from '../../shared/models';

const mockResponse: DotPageApiResponse = {
    page: {
        pageURI: 'test-url',
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
    });

    describe('effects', () => {
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
