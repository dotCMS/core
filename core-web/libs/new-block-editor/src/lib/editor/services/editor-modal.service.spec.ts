import {
    createServiceFactory,
    mockProvider,
    SpectatorService,
    SpyObject
} from '@openng/spectator/jest';
import { Subject } from 'rxjs';

import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';

import { Editor } from '@tiptap/core';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { DotBrowserSelectorComponent } from '@dotcms/ui';

import { EditorModalService } from './editor-modal.service';

import { insertDotAudioFromContentlet } from '../editor.utils';

jest.mock('../editor.utils', () => ({
    insertDotImageFromContentlet: jest.fn(),
    insertDotVideoFromContentlet: jest.fn(),
    insertDotAudioFromContentlet: jest.fn()
}));

const AUDIO_DIALOG_TITLE_KEY = 'dot.block-editor.extension.audio.dotcms.dialog-title';

describe('EditorModalService — openAudioPicker', () => {
    let spectator: SpectatorService<EditorModalService>;
    let service: EditorModalService;
    let dialogService: SpyObject<DialogService>;
    let onClose$: Subject<DotCMSContentlet | undefined>;

    const editor = {} as Editor;
    const insertAudioMock = insertDotAudioFromContentlet as jest.Mock;

    const createService = createServiceFactory({
        service: EditorModalService,
        providers: [
            mockProvider(DialogService),
            mockProvider(DotMessageService, { get: jest.fn((key: string) => key) })
        ]
    });

    beforeEach(() => {
        insertAudioMock.mockClear();
        spectator = createService();
        service = spectator.service;
        dialogService = spectator.inject(DialogService);

        onClose$ = new Subject<DotCMSContentlet | undefined>();
        dialogService.open.mockReturnValue({
            onClose: onClose$.asObservable(),
            close: jest.fn()
        } as unknown as DynamicDialogRef);
    });

    it('opens the browser selector scoped to audio mime types', () => {
        service.openAudioPicker(editor);

        expect(dialogService.open).toHaveBeenCalledTimes(1);
        const [component, config] = dialogService.open.mock.calls[0];
        expect(component).toBe(DotBrowserSelectorComponent);
        expect(config.header).toBe(AUDIO_DIALOG_TITLE_KEY);
        expect(config.data.mimeTypes).toEqual(['audio']);
    });

    it('inserts the picked contentlet as a dotAudio node on close', () => {
        service.openAudioPicker(editor);
        const contentlet = { identifier: 'id-1', inode: 'inode-1' } as DotCMSContentlet;

        onClose$.next(contentlet);

        expect(insertAudioMock).toHaveBeenCalledWith(editor, contentlet);
    });

    it('does nothing when the picker closes without a selection', () => {
        service.openAudioPicker(editor);

        onClose$.next(undefined);

        expect(insertAudioMock).not.toHaveBeenCalled();
    });

    it('is idempotent while the picker is already open', () => {
        service.openAudioPicker(editor);
        service.openAudioPicker(editor);

        expect(dialogService.open).toHaveBeenCalledTimes(1);
    });
});
