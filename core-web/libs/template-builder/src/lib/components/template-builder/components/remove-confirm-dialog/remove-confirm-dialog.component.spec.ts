import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { ConfirmationService } from 'primeng/api';
import { ConfirmPopup } from 'primeng/confirmpopup';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe, DotMessagePipeModule } from '@dotcms/ui';

import { RemoveConfirmDialogComponent } from './remove-confirm-dialog.component';

import { DOT_MESSAGE_SERVICE_TB_MOCK } from '../../utils/mocks';

// todo check for assertions
describe('RemoveConfirmDialogComponent', () => {
    let spectator: Spectator<RemoveConfirmDialogComponent>;

    const createComponent = createComponentFactory({
        component: RemoveConfirmDialogComponent,
        imports: [DotMessagePipeModule],
        providers: [
            ConfirmationService,
            DotMessagePipe,
            {
                provide: DotMessageService,
                useValue: DOT_MESSAGE_SERVICE_TB_MOCK
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
        jest.spyOn(ConfirmPopup.prototype, 'bindScrollListener').mockImplementation(jest.fn());
    });

    it('should emit confirm event and call accept function', async () => {
        const confirmEventSpy = jest.spyOn(spectator.component.deleteConfirmed, 'emit');

        const deleteButton = spectator.query(byTestId('btn-remove-item'));
        spectator.dispatchMouseEvent(deleteButton, 'onClick');

        const confirmAccept = spectator.query('.p-confirm-popup-accept');
        spectator.click(confirmAccept);

        expect(confirmEventSpy).toHaveBeenCalled();
    });

    it('should emit confirm event and call reject function', () => {
        const rejectEventSpy = jest.spyOn(spectator.component.deleteRejected, 'emit');

        const deleteButton = spectator.query(byTestId('btn-remove-item'));
        spectator.dispatchMouseEvent(deleteButton, 'onClick');

        const confirmRejected = spectator.query('.p-confirm-popup-reject');
        spectator.click(confirmRejected);

        expect(rejectEventSpy).toHaveBeenCalled();
    });
});
