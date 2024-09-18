import { expect, it, describe } from '@jest/globals';
import { SpectatorService, createServiceFactory } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotEmaDialogStore } from './dot-ema-dialog.store';

import { DotActionUrlService } from '../../../services/dot-action-url/dot-action-url.service';
import { LAYOUT_URL } from '../../../shared/consts';
import { DialogStatus, FormStatus } from '../../../shared/enums';
import { PAYLOAD_MOCK } from '../../../shared/mocks';
import { DotPage } from '../../../shared/models';

describe('DotEmaDialogStoreService', () => {
    let spectator: SpectatorService<DotEmaDialogStore>;

    const createService = createServiceFactory({
        service: DotEmaDialogStore,
        mocks: [DotActionUrlService],
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'edit.ema.page.dialog.header.search.content': 'Search Content',
                    'edit.ema.page.dialog.header.search.form': 'Search Form',
                    'contenttypes.content.create.contenttype': 'Create {0}'
                })
            }
        ]
    });

    beforeEach(() => {
        spectator = createService();
    });

    it('should update dialog status', (done) => {
        spectator.service.setStatus(DialogStatus.LOADING);

        spectator.service.dialogState$.subscribe((state) => {
            expect(state).toEqual({
                url: '',
                header: '',
                type: null,
                status: DialogStatus.LOADING,
                editContentForm: {
                    status: FormStatus.PRISTINE,
                    isTranslation: false
                }
            });
            done();
        });
    });

    it("should set the form state to 'DIRTY'", (done) => {
        spectator.service.setDirty();

        spectator.service.dialogState$.subscribe((state) => {
            expect(state.editContentForm.status).toBe(FormStatus.DIRTY);
            done();
        });
    });

    it("should set the form state to 'SAVED'", (done) => {
        spectator.service.setSaved();

        spectator.service.dialogState$.subscribe((state) => {
            expect(state.editContentForm.status).toBe(FormStatus.SAVED);
            done();
        });
    });

    it('should reset iframe properties', (done) => {
        spectator.service.setStatus(DialogStatus.LOADING);

        spectator.service.resetDialog();

        spectator.service.dialogState$.subscribe((state) => {
            expect(state).toEqual({
                url: '',
                status: DialogStatus.IDLE,
                header: '',
                type: null,
                payload: undefined,
                editContentForm: {
                    status: FormStatus.PRISTINE,
                    isTranslation: false
                }
            });
            done();
        });
    });

    it('should initialize with edit iframe properties', (done) => {
        spectator.service.editContentlet({
            inode: '123',
            title: 'test'
        });

        const queryParams = new URLSearchParams({
            p_p_id: 'content',
            p_p_action: '1',
            p_p_state: 'maximized',
            p_p_mode: 'view',
            _content_struts_action: '/ext/contentlet/edit_contentlet',
            _content_cmd: 'edit',
            inode: '123'
        });

        spectator.service.dialogState$.subscribe((state) => {
            expect(state).toEqual({
                url: LAYOUT_URL + '?' + queryParams.toString(),
                status: DialogStatus.LOADING,
                header: 'test',
                type: 'content',
                editContentForm: {
                    status: FormStatus.PRISTINE,
                    isTranslation: false
                }
            });
            done();
        });
    });

    it('should initialize with edit iframe properties', (done) => {
        spectator.service.editUrlContentMapContentlet({
            inode: '123',
            title: 'test'
        });

        const queryParams = new URLSearchParams({
            p_p_id: 'content',
            p_p_action: '1',
            p_p_state: 'maximized',
            p_p_mode: 'view',
            _content_struts_action: '/ext/contentlet/edit_contentlet',
            _content_cmd: 'edit',
            inode: '123'
        });

        spectator.service.dialogState$.subscribe((state) => {
            expect(state).toEqual({
                url: LAYOUT_URL + '?' + queryParams.toString() + '&isURLMap=true',
                status: DialogStatus.LOADING,
                header: 'test',
                type: 'content',
                editContentForm: {
                    status: FormStatus.PRISTINE,
                    isTranslation: false
                }
            });
            done();
        });
    });

    it('should initialize with addA iframe properties', (done) => {
        spectator.service.addContentlet({
            containerId: '1234',
            acceptTypes: 'test',
            language_id: '1',
            payload: PAYLOAD_MOCK
        });

        spectator.service.dialogState$.subscribe((state) => {
            expect(state).toEqual({
                url: '/html/ng-contentlet-selector.jsp?ng=true&container_id=1234&add=test&language_id=1',
                header: 'Search Content',
                type: 'content',
                status: DialogStatus.LOADING,
                payload: PAYLOAD_MOCK,
                editContentForm: {
                    status: FormStatus.PRISTINE,
                    isTranslation: false
                }
            });
            done();
        });
    });

    it('should initialize with Form Iframe properties', (done) => {
        spectator.service.addFormContentlet(PAYLOAD_MOCK);

        spectator.service.dialogState$.subscribe((state) => {
            expect(state).toEqual({
                header: 'Search Form',
                status: DialogStatus.LOADING,
                url: null,
                type: 'form',
                payload: PAYLOAD_MOCK,
                editContentForm: {
                    status: FormStatus.PRISTINE,
                    isTranslation: false
                }
            });
            done();
        });
    });

    it('should initialize with create iframe properties', (done) => {
        spectator.service.createContentlet({
            contentType: 'test',
            url: 'some/really/long/url',
            payload: PAYLOAD_MOCK
        });

        spectator.service.dialogState$.subscribe((state) => {
            expect(state).toEqual({
                url: 'some/really/long/url',
                status: DialogStatus.LOADING,
                header: 'Create test',
                type: 'content',
                payload: PAYLOAD_MOCK,
                editContentForm: {
                    status: FormStatus.PRISTINE,
                    isTranslation: false
                }
            });
            done();
        });
    });

    it('should update dialog state', (done) => {
        spectator.service.createContentlet({
            url: 'some/really/long/url',
            contentType: 'Blog Posts',
            payload: PAYLOAD_MOCK
        });

        spectator.service.dialogState$.subscribe((state) => {
            expect(state.header).toBe('Create Blog Posts');
            expect(state.status).toBe(DialogStatus.LOADING);
            expect(state.url).toBe('some/really/long/url');
            expect(state.type).toBe('content');
            expect(state.payload).toEqual(PAYLOAD_MOCK);
            done();
        });
    });

    it('should update state to show dialog for create content from palette', (done) => {
        const dotActionUrlService = spectator.inject(DotActionUrlService);

        dotActionUrlService.getCreateContentletUrl.andReturn(of('https://demo.dotcms.com/jsp.jsp'));

        spectator.service.createContentletFromPalette({
            variable: 'blogPost',
            name: 'Blog',
            payload: PAYLOAD_MOCK
        });

        spectator.service.dialogState$.subscribe((state) => {
            expect(state.header).toBe('Create Blog');
            expect(state.status).toBe(DialogStatus.LOADING);

            expect(state.url).toBe('https://demo.dotcms.com/jsp.jsp');
            expect(state.type).toBe('content');
            expect(state.payload).toEqual(PAYLOAD_MOCK);
            done();
        });

        expect(dotActionUrlService.getCreateContentletUrl).toHaveBeenCalledWith('blogPost');
    });

    it('should initialize with loading iframe properties', (done) => {
        spectator.service.loadingIframe('test');

        spectator.service.dialogState$.subscribe((state) => {
            expect(state).toEqual({
                url: '',
                status: DialogStatus.LOADING,
                header: 'test',
                type: 'content',
                editContentForm: {
                    status: FormStatus.PRISTINE,
                    isTranslation: false
                }
            });
            done();
        });
    });

    it('should update the state to show dialog with a specific URL', (done) => {
        spectator.service.openDialogOnURL({
            url: 'https://demo.dotcms.com/jsp.jsp',
            title: 'test'
        });

        spectator.service.dialogState$.subscribe((state) => {
            expect(state).toEqual({
                url: 'https://demo.dotcms.com/jsp.jsp',
                status: DialogStatus.LOADING,
                header: 'test',
                type: 'content',
                editContentForm: {
                    status: FormStatus.PRISTINE,
                    isTranslation: false
                }
            });
            done();
        });
    });

    describe('Dialog for translation', () => {
        it('should update the state to show dialog for a translation', () => {
            spectator.service.translatePage({
                page: {
                    inode: '123',
                    liveInode: '1234',
                    stInode: '12345',
                    live: true,
                    title: 'test'
                } as DotPage,
                newLanguage: 2
            });

            const queryParams = new URLSearchParams({
                p_p_id: 'content',
                p_p_action: '1',
                p_p_state: 'maximized',
                angularCurrentPortlet: 'edit-page',
                _content_sibbling: '123',
                _content_cmd: 'edit',
                p_p_mode: 'view',
                _content_sibblingStructure: '123',
                _content_struts_action: '/ext/contentlet/edit_contentlet',
                inode: '',
                lang: '2',
                populateaccept: 'true',
                reuseLastLang: 'true'
            });

            spectator.service.dialogState$.subscribe((state) => {
                expect(state).toEqual({
                    url: LAYOUT_URL + '?' + queryParams.toString(),
                    status: DialogStatus.LOADING,
                    header: 'test',
                    type: 'content',
                    editContentForm: {
                        status: FormStatus.PRISTINE,
                        isTranslation: true
                    }
                });
            });
        });

        it('should update the state to show dialog for a translation with working inode', () => {
            spectator.service.translatePage({
                page: {
                    inode: '123',
                    liveInode: '1234',
                    stInode: '12345',
                    live: true,
                    title: 'test',
                    working: true,
                    workingInode: '56789'
                } as DotPage,
                newLanguage: 2
            });

            const queryParams = new URLSearchParams({
                p_p_id: 'content',
                p_p_action: '1',
                p_p_state: 'maximized',
                angularCurrentPortlet: 'edit-page',
                _content_sibbling: '56789',
                _content_cmd: 'edit',
                p_p_mode: 'view',
                _content_sibblingStructure: '56789',
                _content_struts_action: '/ext/contentlet/edit_contentlet',
                inode: '',
                lang: '2',
                populateaccept: 'true',
                reuseLastLang: 'true'
            });

            spectator.service.dialogState$.subscribe((state) => {
                expect(state).toEqual({
                    url: LAYOUT_URL + '?' + queryParams.toString(),
                    status: DialogStatus.LOADING,
                    header: 'test',
                    type: 'content',
                    editContentForm: {
                        status: FormStatus.PRISTINE,
                        isTranslation: true
                    }
                });
            });
        });
    });
});
