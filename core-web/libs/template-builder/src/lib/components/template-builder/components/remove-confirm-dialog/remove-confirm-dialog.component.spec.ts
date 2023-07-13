import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { ConfirmationService } from 'primeng/api';
import { ConfirmPopup } from 'primeng/confirmpopup';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { RemoveConfirmDialogComponent } from './remove-confirm-dialog.component';

import { DOT_MESSAGE_SERVICE_TB_MOCK } from '../../utils/mocks';

describe('RemoveConfirmDialogComponent', () => {
    let spectator: Spectator<RemoveConfirmDialogComponent>;

    const createComponent = createComponentFactory({
        component: RemoveConfirmDialogComponent,
        imports: [DotMessagePipe],
        providers: [
            ConfirmationService,
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

    it('should call reject function when esc is pressed', () => {
        const rejectEventSpy = jest.spyOn(spectator.component.deleteRejected, 'emit');

        const deleteButton = spectator.query(byTestId('btn-remove-item'));
        spectator.dispatchMouseEvent(deleteButton, 'onClick');

        const confirmAccept = spectator.query('.p-confirm-popup-accept');
        expect(confirmAccept).toBeTruthy();

        spectator.component.onEscapePress();
        expect(rejectEventSpy).toHaveBeenCalled();
    });
});
