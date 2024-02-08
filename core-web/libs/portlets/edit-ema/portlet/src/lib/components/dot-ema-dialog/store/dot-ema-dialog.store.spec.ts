import { expect, it, describe } from '@jest/globals';
import { SpectatorService, createServiceFactory } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { DotEmaDialogStore } from './dot-ema-dialog.store';

import { DotActionUrlService } from '../../../services/dot-action-url/dot-action-url.service';
import { EDIT_CONTENTLET_URL, PAYLOAD_MOCK } from '../../../shared/consts';

describe('DotEmaDialogStoreService', () => {
    let spectator: SpectatorService<DotEmaDialogStore>;

    const createService = createServiceFactory({
        service: DotEmaDialogStore,
        mocks: [DotActionUrlService]
    });

    beforeEach(() => {
        spectator = createService();
    });

    it('should update Loading', (done) => {
        spectator.service.setLoading(true);

        spectator.service.dialogState$.subscribe((state) => {
            expect(state).toEqual({
                url: '',
                loading: true,
                header: '',
                type: null,
                visible: false
            });
            done();
        });
    });

    it('should reset iframe properties', (done) => {
        spectator.service.setLoading(true);

        spectator.service.resetDialog();

        spectator.service.dialogState$.subscribe((state) => {
            expect(state).toEqual({
                url: '',
                loading: false,
                header: '',
                type: null,
                visible: false,
                payload: undefined
            });
            done();
        });
    });

    it('should initialize with edit iframe properties', (done) => {
        spectator.service.openEditIframe({
            inode: '123',
            title: 'test'
        });

        spectator.service.dialogState$.subscribe((state) => {
            expect(state).toEqual({
                url: EDIT_CONTENTLET_URL + '123',
                loading: true,
                header: 'test',
                type: 'content',
                visible: true
            });
            done();
        });
    });

    it('should initialize with addA iframe properties', (done) => {
        spectator.service.openAddIframe({
            containerId: '1234',
            acceptTypes: 'test',
            language_id: '1',
            payload: PAYLOAD_MOCK
        });

        spectator.service.dialogState$.subscribe((state) => {
            expect(state).toEqual({
                url: '/html/ng-contentlet-selector.jsp?ng=true&container_id=1234&add=test&language_id=1',
                loading: true,
                header: 'Search Content',
                type: 'content',
                visible: true,
                payload: PAYLOAD_MOCK
            });
            done();
        });
    });

    it('should initialize with Form Iframe properties', (done) => {
        spectator.service.openAddFormIframe(PAYLOAD_MOCK);

        spectator.service.dialogState$.subscribe((state) => {
            expect(state).toEqual({
                header: 'Search Forms', // Does this need translation?
                loading: true,
                url: null,
                type: 'form',
                visible: true,
                payload: PAYLOAD_MOCK
            });
            done();
        });
    });

    it('should initialize with create iframe properties', (done) => {
        spectator.service.openCreateIframe({
            contentType: 'test',
            url: 'some/really/long/url'
        });

        spectator.service.dialogState$.subscribe((state) => {
            expect(state).toEqual({
                url: 'some/really/long/url',
                loading: true,
                header: 'Create test',
                type: 'content',
                visible: true,
                payload: undefined
            });
            done();
        });
    });

    it('should update dialog state', (done) => {
        spectator.service.openCreateIframe({
            url: 'some/really/long/url',
            contentType: 'Blog Posts'
        });

        spectator.service.dialogState$.subscribe((state) => {
            expect(state.header).toBe('Create Blog Posts');
            expect(state.loading).toBe(true);
            expect(state.url).toBe('some/really/long/url');
            expect(state.type).toBe('content');
            expect(state.visible).toBe(true);
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
            expect(state.loading).toBe(true);
            expect(state.url).toBe('https://demo.dotcms.com/jsp.jsp');
            expect(state.type).toBe('content');
            expect(state.visible).toBe(true);
            expect(state.payload).toEqual(PAYLOAD_MOCK);
            done();
        });

        expect(dotActionUrlService.getCreateContentletUrl).toHaveBeenCalledWith('blogPost');
    });
});
