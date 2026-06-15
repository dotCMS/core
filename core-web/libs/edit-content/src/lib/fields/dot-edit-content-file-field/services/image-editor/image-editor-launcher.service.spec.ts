import { Subject } from 'rxjs';

import { TestBed } from '@angular/core/testing';

import { DialogService } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSTempFile } from '@dotcms/dotcms-models';

import { LegacyDialogImageEditorLauncher } from './legacy-dialog-image-editor-launcher.service';
import { LegacyDojoImageEditorLauncher } from './legacy-dojo-image-editor-launcher.service';
import { NoOpImageEditorLauncher } from './no-op-image-editor-launcher.service';

describe('NoOpImageEditorLauncher', () => {
    const launcher = new NoOpImageEditorLauncher();

    it('should report the editor as unavailable', () => {
        expect(launcher.isAvailable()).toBe(false);
    });

    it('should not emit when opened', () => {
        const next = jest.fn();
        const complete = jest.fn();

        launcher.open().subscribe({ next, complete });

        expect(next).not.toHaveBeenCalled();
        expect(complete).toHaveBeenCalled();
    });
});

describe('LegacyDojoImageEditorLauncher', () => {
    const launcher = new LegacyDojoImageEditorLauncher();
    const variable = 'fileAsset';

    it('should report the editor as available', () => {
        expect(launcher.isAvailable()).toBe(true);
    });

    it('should dispatch the open-image-editor event with the asset details', () => {
        const dispatchSpy = jest.spyOn(document, 'dispatchEvent');

        const sub = launcher
            .open({ inode: 'inode-1', tempId: undefined, variable, fieldName: variable })
            .subscribe();

        const event = dispatchSpy.mock.calls
            .map(([e]) => e as CustomEvent)
            .find((e) => e.type === `binaryField-open-image-editor-${variable}`);

        expect(event).toBeTruthy();
        expect(event?.detail).toEqual({ inode: 'inode-1', tempId: undefined, variable });

        sub.unsubscribe();
        dispatchSpy.mockRestore();
    });

    it('should resolve with the temp file emitted by the editor', () => {
        const tempFile = { id: 'edited-temp' } as DotCMSTempFile;
        const next = jest.fn();
        const complete = jest.fn();

        const sub = launcher
            .open({ inode: 'inode-1', variable, fieldName: variable })
            .subscribe({ next, complete });

        document.dispatchEvent(
            new CustomEvent(`binaryField-tempfile-${variable}`, {
                detail: { tempFile }
            })
        );

        expect(next).toHaveBeenCalledWith(tempFile);
        expect(complete).toHaveBeenCalled();

        sub.unsubscribe();
    });

    it('should complete without emitting when the editor is closed', () => {
        const next = jest.fn();
        const complete = jest.fn();

        const sub = launcher
            .open({ inode: 'inode-1', variable, fieldName: variable })
            .subscribe({ next, complete });

        document.dispatchEvent(new CustomEvent(`binaryField-close-image-editor-${variable}`));

        expect(next).not.toHaveBeenCalled();
        expect(complete).toHaveBeenCalled();

        sub.unsubscribe();
    });
});

describe('LegacyDialogImageEditorLauncher', () => {
    let launcher: LegacyDialogImageEditorLauncher;
    let onClose$: Subject<unknown>;
    let closeSpy: jest.Mock;
    let openSpy: jest.Mock;
    const variable = 'fileAsset';

    const postMessage = (data: unknown) =>
        window.dispatchEvent(new MessageEvent('message', { data, origin: window.location.origin }));

    beforeEach(() => {
        onClose$ = new Subject<unknown>();
        closeSpy = jest.fn(() => onClose$.next(undefined));
        openSpy = jest.fn(() => ({ onClose: onClose$.asObservable(), close: closeSpy }));

        TestBed.configureTestingModule({
            providers: [
                LegacyDialogImageEditorLauncher,
                { provide: DialogService, useValue: { open: openSpy } },
                { provide: DotMessageService, useValue: { get: (key: string) => key } }
            ]
        });

        launcher = TestBed.inject(LegacyDialogImageEditorLauncher);
    });

    it('should report the editor as available', () => {
        expect(launcher.isAvailable()).toBe(true);
    });

    it('should open the dialog with the asset details', () => {
        const sub = launcher
            .open({ inode: 'inode-1', tempId: 't1', variable, fieldName: variable })
            .subscribe();

        expect(openSpy).toHaveBeenCalled();
        expect(openSpy.mock.calls[0][1].data).toEqual({ inode: 'inode-1', tempId: 't1', variable });

        sub.unsubscribe();
    });

    it('should emit the temp file forwarded by the editor and close the dialog', () => {
        const tempFile = { id: 'edited-temp' } as DotCMSTempFile;
        const next = jest.fn();
        const complete = jest.fn();

        const sub = launcher
            .open({ inode: 'inode-1', variable, fieldName: variable })
            .subscribe({ next, complete });

        postMessage({ source: 'dot-image-editor', type: 'tempfile', variable, tempFile });

        expect(next).toHaveBeenCalledWith(tempFile);
        expect(closeSpy).toHaveBeenCalled();
        expect(complete).toHaveBeenCalled();

        sub.unsubscribe();
    });

    it('should ignore messages from other sources or variables', () => {
        const next = jest.fn();

        const sub = launcher
            .open({ inode: 'inode-1', variable, fieldName: variable })
            .subscribe({ next });

        postMessage({ source: 'other', type: 'tempfile', variable, tempFile: { id: 'x' } });
        postMessage({
            source: 'dot-image-editor',
            type: 'tempfile',
            variable: 'another',
            tempFile: { id: 'y' }
        });

        expect(next).not.toHaveBeenCalled();

        sub.unsubscribe();
    });

    it('should complete without emitting when the editor is closed', () => {
        const next = jest.fn();
        const complete = jest.fn();

        const sub = launcher
            .open({ inode: 'inode-1', variable, fieldName: variable })
            .subscribe({ next, complete });

        postMessage({ source: 'dot-image-editor', type: 'close', variable });

        expect(next).not.toHaveBeenCalled();
        expect(complete).toHaveBeenCalled();

        sub.unsubscribe();
    });
});
