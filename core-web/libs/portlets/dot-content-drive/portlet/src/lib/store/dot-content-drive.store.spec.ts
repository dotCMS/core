import { describe } from '@jest/globals';
import { createServiceFactory, SpectatorService } from '@ngneat/spectator/jest';

import { DotContentDriveStore } from './dot-content-drive.store';

describe('DotContentDriveStore', () => {
    let spectator: SpectatorService<InstanceType<typeof DotContentDriveStore>>;

    const createService = createServiceFactory({
        service: DotContentDriveStore,
        providers: []
    });

    beforeEach(() => {
        spectator = createService();
    });

    it('should be created', () => {
        expect(spectator.service).toBeTruthy();
    });
});
