import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { ConfirmationService } from 'primeng/api';
import { ConfirmPopup } from 'primeng/confirmpopup';

import { RemoveConfirmDialogComponent } from './remove-confirm-dialog.component';

describe('RemoveConfirmDialogComponent', () => {
    let spectator: Spectator<RemoveConfirmDialogComponent>;

    const createComponent = createComponentFactory({
        component: RemoveConfirmDialogComponent,
        providers: [ConfirmationService]
    });

    beforeEach(() => {
        spectator = createComponent();
        jest.spyOn(ConfirmPopup.prototype, 'bindScrollListener').mockImplementation(jest.fn());
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });

    it('should emit confirm event', () => {
        const confirmMock = jest.spyOn(spectator.component, 'openConfirmationDialog');
        const deleteButton = spectator.query(byTestId('btn-remove-item'));
        spectator.dispatchFakeEvent(deleteButton, 'onClick');

        expect(confirmMock).toHaveBeenCalled();
    });

    it('should emit confirm event and call accept function', async () => {
        const confirmEventSpy = jest.spyOn(spectator.component.deleteConfirmed, 'emit');

        const deleteButton = spectator.query(byTestId('btn-remove-item'));
        spectator.dispatchMouseEvent(deleteButton, 'onClick');

        const confirmAccept = spectator.query('.p-confirm-popup-accept');
        spectator.click(confirmAccept);

        expect(confirmEventSpy).toHaveBeenCalled();
    });
});
