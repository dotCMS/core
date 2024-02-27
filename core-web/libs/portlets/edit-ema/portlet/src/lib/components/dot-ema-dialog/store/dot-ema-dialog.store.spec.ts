import { expect, it, describe } from '@jest/globals';
import { SpectatorService, createServiceFactory } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DialogStatus, DotEmaDialogStore } from './dot-ema-dialog.store';

import { DotActionUrlService } from '../../../services/dot-action-url/dot-action-url.service';
import { EDIT_CONTENTLET_URL, PAYLOAD_MOCK } from '../../../shared/consts';

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
                status: DialogStatus.LOADING
            });
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
                payload: undefined
            });
            done();
        });
    });

    it('should initialize with edit iframe properties', (done) => {
        spectator.service.editContentlet({
            inode: '123',
            title: 'test'
        });

        spectator.service.dialogState$.subscribe((state) => {
            expect(state).toEqual({
                url: EDIT_CONTENTLET_URL + '123',
                status: DialogStatus.LOADING,
                header: 'test',
                type: 'content'
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
                payload: PAYLOAD_MOCK
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
                payload: PAYLOAD_MOCK
            });
            done();
        });
    });

    it('should initialize with create iframe properties', (done) => {
        spectator.service.createContentlet({
            contentType: 'test',
            url: 'some/really/long/url'
        });

        spectator.service.dialogState$.subscribe((state) => {
            expect(state).toEqual({
                url: 'some/really/long/url',
                status: DialogStatus.LOADING,
                header: 'Create test',
                type: 'content',
                payload: undefined
            });
            done();
        });
    });

    it('should update dialog state', (done) => {
        spectator.service.createContentlet({
            url: 'some/really/long/url',
            contentType: 'Blog Posts'
        });

        spectator.service.dialogState$.subscribe((state) => {
            expect(state.header).toBe('Create Blog Posts');
            expect(state.status).toBe(DialogStatus.LOADING);
            expect(state.url).toBe('some/really/long/url');
            expect(state.type).toBe('content');
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
                type: 'content'
            });
            done();
        });
    });
});
