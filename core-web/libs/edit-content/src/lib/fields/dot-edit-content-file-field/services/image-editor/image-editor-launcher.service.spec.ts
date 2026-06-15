import { DotCMSTempFile } from '@dotcms/dotcms-models';

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
