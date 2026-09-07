import {
    createServiceFactory,
    mockProvider,
    SpectatorService,
    SpyObject
} from '@openng/spectator/jest';
import { of, Subject } from 'rxjs';

import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';

import { Editor } from '@tiptap/core';

import { DotMessageService, DotSiteService } from '@dotcms/data-access';
import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { ASSET_PICKER_LAUNCHER, DotBrowserSelectorComponent } from '@dotcms/ui';
import { createFakeContentlet } from '@dotcms/utils-testing';

import { EditorModalService } from './editor-modal.service';

import { buildBrowserSelectorConfig } from '../config.utils';
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

/**
 * The Story Block in the **legacy Dojo editor** — i.e. mounted as the `<dotcms-block-editor>` /
 * `<dotcms-old-block-editor>` custom element, where `ASSET_PICKER_LAUNCHER` is NOT provided.
 *
 * That host is the old edit contentlet page, which the new AssetPicker was never designed for, so
 * image / video / audio must keep opening `DotBrowserSelectorComponent` exactly as they did before
 * the picker landed (#36944).
 *
 * Kept in its own file because Spectator allows a single `createServiceFactory` per file, and this
 * scenario needs a factory that omits the launcher token — which the sibling
 * `editor-modal.service.spec.ts` provides.
 */
describe('EditorModalService — legacy Dojo host (no asset-picker launcher)', () => {
    let spectator: SpectatorService<EditorModalService>;
    let service: EditorModalService;
    let dialogService: SpyObject<DialogService>;
    let siteService: SpyObject<DotSiteService>;
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
            mockProvider(DotSiteService, { getCurrentSite: jest.fn(() => of(null)) }),
            { provide: EditorStore, useValue: { languageId: signal(1) } }
            // ASSET_PICKER_LAUNCHER intentionally not provided (legacy host).
        ]
    });

    beforeEach(() => {
        jest.clearAllMocks();
        spectator = createService();
        service = spectator.service;
        dialogService = spectator.inject(DialogService);
        siteService = spectator.inject(DotSiteService);

        onClose$ = new Subject<DotCMSContentlet | undefined>();
        closeSpy = jest.fn();
        dialogService.open.mockReturnValue({
            onClose: onClose$.asObservable(),
            close: closeSpy
        } as unknown as DynamicDialogRef);
    });

    /** The config object handed to `DialogService.open` for the Nth call. */
    const openedConfig = (call = 0) => dialogService.open.mock.calls[call][1];

    it('should not resolve the launcher token in this host', () => {
        // `TestBed.inject` with `optional` is the only way to ask without throwing NG0201 — which
        // is exactly the shape every consumer uses.
        expect(TestBed.inject(ASSET_PICKER_LAUNCHER, null, { optional: true })).toBeNull();
    });

    describe.each([
        [
            'image',
            'openImagePicker',
            ['image'],
            'dot.block-editor.extension.image.dotcms.dialog-title'
        ],
        [
            'video',
            'openVideoPicker',
            ['video'],
            'dot.block-editor.extension.video.dotcms.dialog-title'
        ],
        [
            'audio',
            'openAudioPicker',
            ['audio'],
            'dot.block-editor.extension.audio.dotcms.dialog-title'
        ]
    ] as const)('%s', (_mode, method, mimeTypes, titleKey) => {
        beforeEach(() => service[method](editor));

        it('should open the legacy browser selector', () => {
            expect(dialogService.open).toHaveBeenCalledTimes(1);
            expect(dialogService.open.mock.calls[0][0]).toBe(DotBrowserSelectorComponent);
        });

        it('should hand it the restored legacy config for this mode', () => {
            // Whole-object rather than field-by-field: what this pins is that the service delegates
            // to `buildBrowserSelectorConfig` with the right header and mime scoping — including
            // the `baseZIndex` that clears the shell's `z-[9998]` backdrop. It cannot notice a
            // field being dropped from the builder itself, since both sides would move together;
            // `config.utils.spec.ts` is what guards that.
            expect(openedConfig()).toEqual(
                buildBrowserSelectorConfig({ header: titleKey, mimeTypes: [...mimeTypes] })
            );
        });
    });

    it('should never look up a site — the legacy picker browses without one', () => {
        service.openImagePicker(editor);
        service.openVideoPicker(editor);

        expect(siteService.getCurrentSite).not.toHaveBeenCalled();
    });

    describe('inserting the picked asset', () => {
        it.each([
            ['openImagePicker', () => insertImage, () => [insertVideo, insertAudio]],
            ['openVideoPicker', () => insertVideo, () => [insertImage, insertAudio]],
            ['openAudioPicker', () => insertAudio, () => [insertImage, insertVideo]]
        ] as const)('should insert the node %s corresponds to', (method, expected, others) => {
            const contentlet = createFakeContentlet({ identifier: 'id-1', inode: 'inode-1' });

            service[method](editor);
            onClose$.next(contentlet);

            expect(expected()).toHaveBeenCalledWith(editor, contentlet);
            others().forEach((fn) => expect(fn).not.toHaveBeenCalled());
        });

        it('should do nothing when the selector closes without a selection', () => {
            service.openImagePicker(editor);
            onClose$.next(undefined);

            expect(insertImage).not.toHaveBeenCalled();
        });
    });

    describe('opening twice', () => {
        it('should not stack a second dialog while one is open', () => {
            service.openImagePicker(editor);
            service.openImagePicker(editor);

            expect(dialogService.open).toHaveBeenCalledTimes(1);
        });

        it('should open again after the first dialog closed', () => {
            service.openImagePicker(editor);
            onClose$.next(undefined);
            service.openImagePicker(editor);

            expect(dialogService.open).toHaveBeenCalledTimes(2);
        });
    });

    it('should close every open selector when the editor unmounts', () => {
        service.openImagePicker(editor);
        service.openVideoPicker(editor);
        service.ngOnDestroy();

        expect(closeSpy).toHaveBeenCalledTimes(2);
    });
});
