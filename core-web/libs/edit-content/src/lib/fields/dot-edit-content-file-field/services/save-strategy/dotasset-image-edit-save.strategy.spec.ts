import { createServiceFactory, SpectatorService } from '@ngneat/spectator/jest';

import { DotCMSTempFile } from '@dotcms/dotcms-models';

import { DotAssetImageEditSaveStrategy } from './dotasset-image-edit-save.strategy';

describe('DotAssetImageEditSaveStrategy', () => {
    let spectator: SpectatorService<DotAssetImageEditSaveStrategy>;

    const createService = createServiceFactory({
        service: DotAssetImageEditSaveStrategy
    });

    beforeEach(() => {
        spectator = createService();
    });

    // Scaffold: the versioned check-in of the referenced dotAsset lands in a
    // follow-up step. For now apply() must be a safe no-op that never falls back
    // to the Binary inline write (which would corrupt the identifier reference).
    it('does not throw when applying (scaffolded no-op)', () => {
        const tempFile = { id: 'temp-1', fileName: 'edited.png' } as DotCMSTempFile;

        expect(() => spectator.service.apply(tempFile)).not.toThrow();
    });
});
