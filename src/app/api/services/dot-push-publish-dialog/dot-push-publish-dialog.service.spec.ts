import { DotPushPublishDialogService } from '@services/dot-push-publish-dialog/dot-push-publish-dialog.service';

const mockEventData = { assetIdentifier: 'test' };

describe('DotPushPublishDialogService', () => {
    const dotPushPublishDialogService = new DotPushPublishDialogService();
    let data;

    beforeEach(() => {
        dotPushPublishDialogService.showDialog$.subscribe(eventData => {
            data = eventData;
        });
    });

    it('should receive data', () => {
        dotPushPublishDialogService.open(mockEventData);
        expect(data).toEqual(mockEventData);
    });
});
