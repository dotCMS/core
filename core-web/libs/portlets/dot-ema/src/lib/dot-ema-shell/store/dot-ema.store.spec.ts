import { describe, expect } from '@jest/globals';
import { createServiceFactory, SpectatorService, SpyObject } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { mockDotContainers, mockDotLayout, mockDotTemplate } from '@dotcms/utils-testing';

import { EditEmaStore } from './dot-ema.store';

import { DotActionUrlService } from '../../services/dot-action-url/dot-action-url.service';
import { DotPageApiResponse, DotPageApiService } from '../../services/dot-page-api.service';
import { DEFAULT_PERSONA, EDIT_CONTENTLET_URL } from '../../shared/consts';

const mockResponse: DotPageApiResponse = {
    page: {
        title: 'Test Page',
        identifier: '123'
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
    layout: mockDotLayout(),
    template: mockDotTemplate(),
    containers: mockDotContainers()
};

describe('EditEmaStore', () => {
    let spectator: SpectatorService<EditEmaStore>;
    let dotPageApiService: SpyObject<DotPageApiService>;
    const createService = createServiceFactory({
        service: EditEmaStore,
        mocks: [DotPageApiService, DotActionUrlService]
    });

    beforeEach(() => {
        spectator = createService();
        mockResponse;
        dotPageApiService = spectator.inject(DotPageApiService);
        dotPageApiService.get.andReturn(of(mockResponse));

        spectator.service.load({ language_id: '1', url: 'test-url', persona_id: '123' });
    });

    describe('selectors', () => {
        it('should return editorState', (done) => {
            spectator.service.editorState$.subscribe((state) => {
                expect(state).toEqual({
                    editor: mockResponse,
                    apiURL: 'http://localhost/api/v1/page/json/test-url?language_id=1&com.dotmarketing.persona.id=modes.persona.no.persona',
                    iframeURL:
                        'http://localhost:3000/test-url?language_id=1&com.dotmarketing.persona.id=modes.persona.no.persona'
                });
                done();
            });
        });
    });

    describe('updaters', () => {
        it('should update editIframeLoading', (done) => {
            spectator.service.setDialogIframeLoading(true);

            spectator.service.state$.subscribe((state) => {
                expect(state).toEqual({
                    editor: mockResponse,
                    url: 'test-url',
                    dialogIframeURL: '',
                    dialogIframeLoading: true,
                    dialogHeader: '',
                    dialogVisible: false
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
                    url: 'test-url',
                    dialogIframeURL: '',
                    dialogIframeLoading: false,
                    dialogHeader: '',
                    dialogVisible: false
                });
                done();
            });
        });

        it('should initialize editAction properties', (done) => {
            spectator.service.initActionEdit({
                inode: '123',
                title: 'test'
            });

            spectator.service.state$.subscribe((state) => {
                expect(state).toEqual({
                    editor: mockResponse,
                    url: 'test-url',
                    dialogIframeURL: EDIT_CONTENTLET_URL + '123',
                    dialogIframeLoading: true,
                    dialogHeader: 'test',
                    dialogVisible: true
                });
                done();
            });
        });

        it('should initialize addAction properties', (done) => {
            spectator.service.initActionAdd({
                containerId: '123',
                acceptTypes: 'test',
                language_id: '1'
            });

            spectator.service.state$.subscribe((state) => {
                expect(state).toEqual({
                    editor: mockResponse,
                    url: 'test-url',
                    dialogIframeURL:
                        '/html/ng-contentlet-selector.jsp?ng=true&container_id=123&add=test&language_id=1',
                    dialogIframeLoading: true,
                    dialogHeader: 'Search Content',
                    dialogVisible: true
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
                    url: 'test-url',
                    dialogIframeURL: 'some/really/long/url',
                    dialogIframeLoading: true,
                    dialogHeader: 'test',
                    dialogVisible: true
                });
                done();
            });
        });

        it('should update dialog state', (done) => {
            spectator.service.setDialog({
                url: 'some/really/long/url',
                title: 'test'
            });

            spectator.service.state$.subscribe((state) => {
                expect(state.dialogHeader).toBe('test');
                expect(state.dialogIframeLoading).toBe(true);
                expect(state.dialogIframeURL).toBe('some/really/long/url');
                expect(state.dialogVisible).toBe(true);
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
                        identifier: '123'
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

            spectator.service.load({ language_id: 'en', url: 'test-url', persona_id: '123' });

            spectator.service.createContentFromPalette({
                variable: 'blogPost',
                name: 'Blog'
            });

            spectator.service.state$.subscribe((state) => {
                expect(state.dialogHeader).toBe('Create Blog');
                expect(state.dialogIframeLoading).toBe(true);
                expect(state.dialogIframeURL).toBe('https://demo.dotcms.com/jsp.jsp');
                expect(state.dialogVisible).toBe(true);
                done();
            });

            expect(dotActionUrlService.getCreateContentletUrl).toHaveBeenCalledWith('blogPost');
        });

        it('should handle successful data loading', (done) => {
            const dotPageApiService = spectator.inject(DotPageApiService);

            dotPageApiService.get.andReturn(of(mockResponse));

            spectator.service.load({ language_id: 'en', url: 'test-url', persona_id: '123' });

            spectator.service.state$.subscribe((state) => {
                expect(state as unknown).toEqual({
                    url: 'test-url',
                    editor: mockResponse,
                    dialogIframeURL: '',
                    dialogIframeLoading: false,
                    dialogHeader: '',
                    dialogVisible: false
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

            spectator.service.load({ language_id: 'en', url: 'test-url', persona_id: '123' });
            spectator.service.savePage({
                pageContainers: [],
                pageId: '789'
            });

            expect(dotPageApiService.save).toHaveBeenCalledWith({
                pageContainers: [],
                pageId: '789'
            });
        });
    });
});
