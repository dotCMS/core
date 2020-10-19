import { TestBed } from '@angular/core/testing';

import { DotPushPublishDialogService } from './dot-push-publish-dialog.service';
import { DotPushPublishDialogData } from '@models/dot-push-publish-dialog-data/dot-push-publish-dialog-data.model';

const mockEventData = { assetIdentifier: 'test', title: 'Title' };

describe('DotPushPublishDialogService', () => {
    const dotPushPublishDialogService = TestBed.get(DotPushPublishDialogService);
    let data;

    beforeEach(() => {
        TestBed.configureTestingModule({});
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
