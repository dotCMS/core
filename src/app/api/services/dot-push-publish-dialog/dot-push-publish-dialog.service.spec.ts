import { DotPushPublishDialogService } from '@services/dot-push-publish-dialog/dot-push-publish-dialog.service';
import { DotPushPublishDialogData } from '@models/dot-push-publish-dialog-data/dot-push-publish-dialog-data.model';

const mockEventData = { assetIdentifier: 'test', title: 'Title' };

describe('DotPushPublishDialogService', () => {
    const dotPushPublishDialogService = new DotPushPublishDialogService();
    let data;

    beforeEach(() => {
        dotPushPublishDialogService.showDialog$.subscribe(
            (dialogData: DotPushPublishDialogData) => {
                data = dialogData;
            }
        );
    });

    it('should receive data', () => {
        dotPushPublishDialogService.open(mockEventData);
        expect(data).toEqual(mockEventData);
    });
});
