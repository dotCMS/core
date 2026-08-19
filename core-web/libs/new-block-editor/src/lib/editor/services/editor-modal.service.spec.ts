import {
    createServiceFactory,
    mockProvider,
    SpectatorService,
    SpyObject
} from '@openng/spectator/jest';
import { Observable, of, Subject, throwError } from 'rxjs';

import { signal } from '@angular/core';

import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';

import { Editor } from '@tiptap/core';

import { DotMessageService, DotSiteService } from '@dotcms/data-access';
import { DotCMSContentlet, DotSite } from '@dotcms/dotcms-models';
import { DotAssetPickerComponent } from '@dotcms/ui';

import { EditorModalService } from './editor-modal.service';

import { OVERLAY_ABOVE_FULLSCREEN_Z_INDEX } from '../config.utils';
import {
    insertDotAudioFromContentlet,
    insertDotImageFromContentlet,
    insertDotVideoFromContentlet
} from '../editor.utils';
import { EditorStore } from '../store/editor.store';

jest.mock('../editor.utils', () => ({
    insertDotImageFromContentlet: jest.fn(),
    insertDotVideoFromContentlet: jest.fn(),
    insertDotAudioFromContentlet: jest.fn()
}));

const SITE: DotSite = {
    identifier: 'site-1',
    hostname: 'dotcms.com',
    aliases: null,
    archived: false
};

const LANGUAGE_ID = 2;

/**
 * Swapped per test so the site lookup can be resolved, deferred or failed. Read lazily by the mock
 * so it can be set before the service is constructed — `currentSite$` is a field initializer.
 */
let siteSource: Observable<DotSite>;

describe('EditorModalService — asset pickers', () => {
    let spectator: SpectatorService<EditorModalService>;
    let service: EditorModalService;
    let dialogService: SpyObject<DialogService>;
    let onClose$: Subject<DotCMSContentlet | undefined>;
    let closeSpy: jest.Mock;

    const editor = {} as Editor;

    const insertImage = insertDotImageFromContentlet as jest.Mock;
    const insertVideo = insertDotVideoFromContentlet as jest.Mock;
    const insertAudio = insertDotAudioFromContentlet as jest.Mock;

    const createService = createServiceFactory({
        service: EditorModalService,
        providers: [
            mockProvider(DialogService),
            mockProvider(DotMessageService, { get: jest.fn((key: string) => key) }),
            mockProvider(DotSiteService, { getCurrentSite: jest.fn(() => siteSource) }),
            { provide: EditorStore, useValue: { languageId: signal(LANGUAGE_ID) } }
        ]
    });

    /** Builds the service against whatever `siteSource` currently is. */
    const setup = () => {
        spectator = createService();
        service = spectator.service;
        dialogService = spectator.inject(DialogService);

        onClose$ = new Subject<DotCMSContentlet | undefined>();
        closeSpy = jest.fn();
        dialogService.open.mockReturnValue({
            onClose: onClose$.asObservable(),
            close: closeSpy
        } as unknown as DynamicDialogRef);
    };

    /** The config object handed to `DialogService.open` for the Nth call. */
    const openedConfig = (call = 0) => dialogService.open.mock.calls[call][1];

    beforeEach(() => {
        jest.clearAllMocks();
        siteSource = of(SITE);
    });

    describe.each([
        ['image', 'openImagePicker', ['image/*'], 'dot.asset.picker.header.image'],
        ['video', 'openVideoPicker', ['video/*'], 'dot.asset.picker.header.video'],
        ['audio', 'openAudioPicker', ['audio/*'], 'dot.asset.picker.header.audio']
    ] as const)('%s', (_mode, method, mimeTypes, titleKey) => {
        beforeEach(() => {
            setup();
            service[method](editor);
        });

        it('should open the shared asset picker', () => {
            expect(dialogService.open).toHaveBeenCalledTimes(1);
            expect(dialogService.open.mock.calls[0][0]).toBe(DotAssetPickerComponent);
        });

        it('should restrict the picker to its own mime types', () => {
            expect(openedConfig().data.mimeTypes).toEqual(mimeTypes);
        });

        it('should title the picker for what it is picking', () => {
            // The picker draws its own header, so the title travels in `data`, not `header`.
            expect(openedConfig().data.title).toBe(titleKey);
            expect(openedConfig().showHeader).toBe(false);
        });

        it('should browse the current site in the editor locale', () => {
            expect(openedConfig().data.site).toBe(SITE);
            expect(openedConfig().data.languageId).toBe(String(LANGUAGE_ID));
        });

        it('should clear the fullscreen editor shell backdrop', () => {
            // Without this the modal renders under the shell's `z-[9998]` and is unreachable.
            expect(openedConfig().baseZIndex).toBe(OVERLAY_ABOVE_FULLSCREEN_Z_INDEX);
        });
    });

    describe('inserting the picked asset', () => {
        it.each([
            ['openImagePicker', () => insertImage, () => [insertVideo, insertAudio]],
            ['openVideoPicker', () => insertVideo, () => [insertImage, insertAudio]],
            ['openAudioPicker', () => insertAudio, () => [insertImage, insertVideo]]
        ] as const)('should insert the node %s corresponds to', (method, expected, others) => {
            setup();
            const contentlet = { identifier: 'id-1', inode: 'inode-1' } as DotCMSContentlet;

            service[method](editor);
            onClose$.next(contentlet);

            expect(expected()).toHaveBeenCalledWith(editor, contentlet);
            others().forEach((fn) => expect(fn).not.toHaveBeenCalled());
        });

        it('should do nothing when the picker closes without a selection', () => {
            setup();

            service.openImagePicker(editor);
            onClose$.next(undefined);

            expect(insertImage).not.toHaveBeenCalled();
        });
    });

    describe('opening twice', () => {
        it('should not stack a second dialog while one is open', () => {
            setup();

            service.openImagePicker(editor);
            service.openImagePicker(editor);

            expect(dialogService.open).toHaveBeenCalledTimes(1);
        });

        it('should not stack a second dialog while the site lookup is still in flight', () => {
            // The guard the async site lookup makes necessary: the ref does not exist yet, so
            // without a pending flag both clicks would sail past it.
            const site$ = new Subject<DotSite>();
            siteSource = site$.asObservable();
            setup();

            service.openImagePicker(editor);
            service.openImagePicker(editor);
            site$.next(SITE);

            expect(dialogService.open).toHaveBeenCalledTimes(1);
        });

        it('should open again after the first dialog closed', () => {
            setup();

            service.openImagePicker(editor);
            onClose$.next(undefined);
            service.openImagePicker(editor);

            expect(dialogService.open).toHaveBeenCalledTimes(2);
        });

        it('should let a different media type open alongside', () => {
            setup();

            service.openImagePicker(editor);
            service.openVideoPicker(editor);

            expect(dialogService.open).toHaveBeenCalledTimes(2);
        });
    });

    describe('site lookup', () => {
        it('should resolve the site once for every picker', () => {
            setup();

            service.openImagePicker(editor);
            onClose$.next(undefined);
            service.openVideoPicker(editor);

            // Cached: the current site cannot change while the editor is mounted.
            expect(spectator.inject(DotSiteService).getCurrentSite).toHaveBeenCalledTimes(1);
        });

        it('should not open a picker that has nothing to browse', () => {
            siteSource = throwError(() => new Error('no site'));
            setup();

            service.openImagePicker(editor);

            expect(dialogService.open).not.toHaveBeenCalled();
        });

        it('should retry the lookup after it failed', () => {
            siteSource = throwError(() => new Error('no site'));
            setup();

            service.openImagePicker(editor);
            service.openImagePicker(editor);

            // The pending flag has to be released on error, or the picker is dead for the session.
            expect(spectator.inject(DotSiteService).getCurrentSite).toHaveBeenCalledTimes(2);
        });
    });

    it('should close every open picker when the editor unmounts', () => {
        setup();

        service.openImagePicker(editor);
        service.openVideoPicker(editor);
        service.ngOnDestroy();

        expect(closeSpy).toHaveBeenCalledTimes(2);
    });
});
