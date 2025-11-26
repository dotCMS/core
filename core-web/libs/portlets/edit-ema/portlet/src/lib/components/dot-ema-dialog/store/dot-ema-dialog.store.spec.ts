import { expect, it, describe } from '@jest/globals';
import { SpectatorService, createServiceFactory } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { signal } from '@angular/core';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSPage, DotCMSUVEAction } from '@dotcms/types';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotEmaDialogStore } from './dot-ema-dialog.store';

import { DotActionUrlService } from '../../../services/dot-action-url/dot-action-url.service';
import { LAYOUT_URL } from '../../../shared/consts';
import { DialogStatus, FormStatus } from '../../../shared/enums';
import { PAYLOAD_MOCK } from '../../../shared/mocks';
import { UVEStore } from '../../../store/dot-uve.store';

const TEST_VARIANT = 'my-test-variant';

describe('DotEmaDialogStoreService', () => {
    let spectator: SpectatorService<DotEmaDialogStore>;

    const createService = createServiceFactory({
        service: DotEmaDialogStore,
        mocks: [DotActionUrlService],
        providers: [
            {
                provide: UVEStore,
                useValue: {
                    pageParams: signal({
                        variantName: TEST_VARIANT // Is the only thing we need to test the component
                    })
                }
            },

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
                form: {
                    status: FormStatus.PRISTINE,
                    isTranslation: false
                },
                clientAction: DotCMSUVEAction.NOOP
            });
            done();
        });
    });

    it("should set the form state to 'DIRTY'", (done) => {
        spectator.service.setDirty();

        spectator.service.dialogState$.subscribe((state) => {
            expect(state.form.status).toBe(FormStatus.DIRTY);
            done();
        });
    });

    it("should set the form state to 'SAVED'", (done) => {
        spectator.service.setSaved();

        spectator.service.dialogState$.subscribe((state) => {
            expect(state.form.status).toBe(FormStatus.SAVED);
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
                actionPayload: undefined,
                form: {
                    status: FormStatus.PRISTINE,
                    isTranslation: false
                },
                clientAction: DotCMSUVEAction.NOOP
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
            inode: '123',
            angularCurrentPortlet: 'undefined',
            variantName: TEST_VARIANT
        });

        spectator.service.dialogState$.subscribe((state) => {
            expect(state).toEqual({
                url: LAYOUT_URL + '?' + queryParams.toString(),
                status: DialogStatus.LOADING,
                header: 'test',
                type: 'content',
                form: {
                    status: FormStatus.PRISTINE,
                    isTranslation: false
                },
                clientAction: DotCMSUVEAction.NOOP
            });
            done();
        });
    });

    it('should initialize with edit iframe properties and with clientAction', (done) => {
        spectator.service.editContentlet({
            inode: '123',
            title: 'test',
            clientAction: DotCMSUVEAction.EDIT_CONTENTLET
        });

        const queryParams = new URLSearchParams({
            p_p_id: 'content',
            p_p_action: '1',
            p_p_state: 'maximized',
            p_p_mode: 'view',
            _content_struts_action: '/ext/contentlet/edit_contentlet',
            _content_cmd: 'edit',
            inode: '123',
            angularCurrentPortlet: 'undefined',
            variantName: TEST_VARIANT
        });

        spectator.service.dialogState$.subscribe((state) => {
            expect(state).toEqual({
                url: LAYOUT_URL + '?' + queryParams.toString(),
                status: DialogStatus.LOADING,
                header: 'test',
                type: 'content',
                form: {
                    status: FormStatus.PRISTINE,
                    isTranslation: false
                },
                clientAction: DotCMSUVEAction.EDIT_CONTENTLET
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
            inode: '123',
            angularCurrentPortlet: null,
            variantName: TEST_VARIANT
        });

        spectator.service.dialogState$.subscribe((state) => {
            expect(state).toEqual({
                url: LAYOUT_URL + '?' + queryParams.toString() + '&isURLMap=true',
                status: DialogStatus.LOADING,
                header: 'test',
                type: 'content',
                form: {
                    status: FormStatus.PRISTINE,
                    isTranslation: false
                },
                clientAction: DotCMSUVEAction.NOOP
            });
            done();
        });
    });

    it('should initialize with addA iframe properties', (done) => {
        spectator.service.addContentlet({
            containerId: '1234',
            acceptTypes: 'test',
            language_id: '1',
            actionPayload: PAYLOAD_MOCK
        });

        spectator.service.dialogState$.subscribe((state) => {
            expect(state).toEqual({
                url:
                    '/html/ng-contentlet-selector.jsp?ng=true&container_id=1234&add=test&language_id=1&' +
                    new URLSearchParams({ variantName: TEST_VARIANT }).toString(),
                header: 'Search Content',
                type: 'content',
                status: DialogStatus.LOADING,
                actionPayload: PAYLOAD_MOCK,
                form: {
                    status: FormStatus.PRISTINE,
                    isTranslation: false
                },
                clientAction: DotCMSUVEAction.NOOP
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
                actionPayload: PAYLOAD_MOCK,
                form: {
                    status: FormStatus.PRISTINE,
                    isTranslation: false
                },
                clientAction: DotCMSUVEAction.NOOP
            });
            done();
        });
    });

    it('should initialize with create iframe properties', (done) => {
        spectator.service.createContentlet({
            contentType: 'test',
            url: 'some/really/long/url',
            actionPayload: PAYLOAD_MOCK
        });

        spectator.service.dialogState$.subscribe((state) => {
            expect(state).toEqual({
                url:
                    'http://localhost/some/really/long/url?' +
                    new URLSearchParams({ variantName: TEST_VARIANT }).toString(),
                status: DialogStatus.LOADING,
                header: 'Create test',
                type: 'content',
                actionPayload: PAYLOAD_MOCK,
                form: {
                    status: FormStatus.PRISTINE,
                    isTranslation: false
                },
                clientAction: DotCMSUVEAction.NOOP
            });
            done();
        });
    });

    it('should update dialog state', (done) => {
        spectator.service.createContentlet({
            url: 'some/really/long/url',
            contentType: 'Blog Posts',
            actionPayload: PAYLOAD_MOCK
        });

        spectator.service.dialogState$.subscribe((state) => {
            expect(state.header).toBe('Create Blog Posts');
            expect(state.status).toBe(DialogStatus.LOADING);
            expect(state.url).toBe(
                'http://localhost/some/really/long/url?' +
                    new URLSearchParams({ variantName: TEST_VARIANT }).toString()
            );
            expect(state.type).toBe('content');
            expect(state.actionPayload).toEqual(PAYLOAD_MOCK);
            done();
        });
    });

    it('should update state to show dialog for create content from palette', (done) => {
        const dotActionUrlService = spectator.inject(DotActionUrlService);

        dotActionUrlService.getCreateContentletUrl.andReturn(of('https://demo.dotcms.com/jsp.jsp'));

        spectator.service.createContentletFromPalette({
            variable: 'blogPost',
            name: 'Blog',
            actionPayload: PAYLOAD_MOCK,
            language_id: 2
        });

        spectator.service.dialogState$.subscribe((state) => {
            expect(state.header).toBe('Create Blog');
            expect(state.status).toBe(DialogStatus.LOADING);

            expect(state.url).toBe(
                'https://demo.dotcms.com/jsp.jsp?' +
                    new URLSearchParams({ variantName: TEST_VARIANT }).toString()
            );
            expect(state.type).toBe('content');
            expect(state.actionPayload).toEqual(PAYLOAD_MOCK);
            done();
        });

        expect(dotActionUrlService.getCreateContentletUrl).toHaveBeenCalledWith('blogPost', 2);
    });

    it('should initialize with loading iframe properties', (done) => {
        spectator.service.loadingIframe('test');

        spectator.service.dialogState$.subscribe((state) => {
            expect(state).toEqual({
                url: '',
                status: DialogStatus.LOADING,
                header: 'test',
                type: 'content',
                form: {
                    status: FormStatus.PRISTINE,
                    isTranslation: false
                },
                clientAction: DotCMSUVEAction.NOOP
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
                form: {
                    status: FormStatus.PRISTINE,
                    isTranslation: false
                },
                clientAction: DotCMSUVEAction.NOOP
            });
            done();
        });
    });

    it('should reset action payload', (done) => {
        // First set an action payload
        spectator.service.addContentlet({
            containerId: '1234',
            acceptTypes: 'test',
            language_id: '1',
            actionPayload: PAYLOAD_MOCK
        });

        // Then reset it
        spectator.service.resetActionPayload();

        spectator.service.dialogState$.subscribe((state) => {
            expect(state.actionPayload).toBeUndefined();
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
                } as DotCMSPage,
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
                reuseLastLang: 'true',
                variantName: TEST_VARIANT
            });

            spectator.service.dialogState$.subscribe((state) => {
                expect(state).toEqual({
                    url: LAYOUT_URL + '?' + queryParams.toString(),
                    status: DialogStatus.LOADING,
                    header: 'test',
                    type: 'content',
                    form: {
                        status: FormStatus.PRISTINE,
                        isTranslation: true
                    },
                    clientAction: DotCMSUVEAction.NOOP
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
                } as DotCMSPage,
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
                reuseLastLang: 'true',
                variantName: TEST_VARIANT
            });

            spectator.service.dialogState$.subscribe((state) => {
                expect(state).toEqual({
                    url: LAYOUT_URL + '?' + queryParams.toString(),
                    status: DialogStatus.LOADING,
                    header: 'test',
                    type: 'content',
                    form: {
                        status: FormStatus.PRISTINE,
                        isTranslation: true
                    },
                    clientAction: DotCMSUVEAction.NOOP
                });
            });
        });
    });
});
