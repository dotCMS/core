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

    it('should emit confirm event and call accept function', () => {
        const confirmEventSpy = jest.spyOn(spectator.component.deleteConfirmed, 'emit');
        const confirmationService = spectator.inject(ConfirmationService);
        const confirmSpy = jest.spyOn(confirmationService, 'confirm').mockImplementation((options) => {
            // Simulate accept callback
            if (options.accept) {
                options.accept();
            }
            return {} as ConfirmationService;
        }) as jest.Mock;

        const deleteButton = spectator.query(byTestId('btn-remove-item'));
        spectator.click(deleteButton);

        expect(confirmSpy).toHaveBeenCalled();
        expect(confirmEventSpy).toHaveBeenCalled();
    });

    it('should emit confirm event and call reject function', () => {
        const rejectEventSpy = jest.spyOn(spectator.component.deleteRejected, 'emit');
        const confirmationService = spectator.inject(ConfirmationService);
        const confirmSpy = jest.spyOn(confirmationService, 'confirm').mockImplementation((options) => {
            // Simulate reject callback
            if (options.reject) {
                options.reject();
            }
            return {} as ConfirmationService;
        }) as jest.Mock;

        const deleteButton = spectator.query(byTestId('btn-remove-item'));
        spectator.click(deleteButton);

        expect(confirmSpy).toHaveBeenCalled();
        expect(rejectEventSpy).toHaveBeenCalled();
    });

    it('should call reject function when esc is pressed', () => {
        const rejectEventSpy = jest.spyOn(spectator.component.deleteRejected, 'emit');

        const deleteButton = spectator.query(byTestId('btn-remove-item'));
        spectator.click(deleteButton);
        spectator.detectChanges();

        spectator.component.onEscapePress();
        expect(rejectEventSpy).toHaveBeenCalled();
    });

    it('should emit confirmation when button is clicked and skipConfirmation is set to true', () => {
        spectator.component.skipConfirmation = true;
        const confirmEventSpy = jest.spyOn(spectator.component.deleteConfirmed, 'emit');

        const deleteButton = spectator.query(byTestId('btn-remove-item'));
        spectator.dispatchMouseEvent(deleteButton, 'onClick');

        expect(confirmEventSpy).toHaveBeenCalled();
    });

    it('should not emit confirmation when button is clicked and skipConfirmation is set to false', () => {
        spectator.component.skipConfirmation = false;
        const confirmEventSpy = jest.spyOn(spectator.component.deleteConfirmed, 'emit');

        const deleteButton = spectator.query(byTestId('btn-remove-item'));
        spectator.dispatchMouseEvent(deleteButton, 'onClick');

        expect(confirmEventSpy).toHaveBeenCalledTimes(0);
    });
});
