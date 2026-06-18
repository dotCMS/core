import { expect } from '@jest/globals';
import { createServiceFactory, SpectatorService } from '@ngneat/spectator/jest';

import { DotCMSTempFile } from '@dotcms/dotcms-models';

import { NoopImageEditorLauncher } from './noop-image-editor.launcher';

describe('NoopImageEditorLauncher', () => {
    let spectator: SpectatorService<NoopImageEditorLauncher>;

    const createService = createServiceFactory(NoopImageEditorLauncher);

    beforeEach(() => {
        spectator = createService();
    });

    it('should report itself as unavailable', () => {
        expect(spectator.service.isAvailable()).toBe(false);
    });

    it('should emit null when opened', () => {
        let result: DotCMSTempFile | null | undefined;

        spectator.service.open().subscribe((value) => (result = value));

        expect(result).toBeNull();
    });
});
