import { createServiceFactory, mockProvider, SpectatorService } from '@ngneat/spectator/jest';

import { DotCMSTempFile } from '@dotcms/dotcms-models';

import { BinaryImageEditSaveStrategy } from './binary-image-edit-save.strategy';

import { FileFieldStore } from '../../store/file-field.store';

describe('BinaryImageEditSaveStrategy', () => {
    let spectator: SpectatorService<BinaryImageEditSaveStrategy>;

    const createService = createServiceFactory({
        service: BinaryImageEditSaveStrategy,
        providers: [mockProvider(FileFieldStore, { applyTempFile: jest.fn() })]
    });

    beforeEach(() => {
        spectator = createService();
    });

    it('applies the edited temp file inline via the store', () => {
        const tempFile = { id: 'temp-1', fileName: 'edited.png' } as DotCMSTempFile;
        const store = spectator.inject(FileFieldStore);

        spectator.service.apply(tempFile);

        expect(store.applyTempFile).toHaveBeenCalledWith(tempFile);
    });
});
