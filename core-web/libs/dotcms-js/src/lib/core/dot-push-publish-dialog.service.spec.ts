import { createServiceFactory, SpectatorService } from '@ngneat/spectator';

import { DotPushPublishDialogService } from './dot-push-publish-dialog.service';

const mockEventData = { assetIdentifier: 'test', title: 'Title' };

describe('DotPushPublishDialogService', () => {
    let spectator: SpectatorService<DotPushPublishDialogService>;
    const createService = createServiceFactory(DotPushPublishDialogService);

    beforeEach(() => {
        spectator = createService();
    });

    it('should receive data', () => {
        spectator.service.showDialog$.subscribe((dialogData) => {
            expect(dialogData).toEqual(mockEventData);
        });
        spectator.service.open(mockEventData);
    });
});
