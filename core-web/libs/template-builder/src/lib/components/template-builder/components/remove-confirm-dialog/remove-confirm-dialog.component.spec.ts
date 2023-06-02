import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { ConfirmationService } from 'primeng/api';

import { RemoveConfirmDialogComponent } from './remove-confirm-dialog.component';

describe('RemoveConfirmDialogComponent', () => {
    let spectator: Spectator<RemoveConfirmDialogComponent>;
    let mockConfirmationService: ConfirmationService;

    const createComponent = createComponentFactory({
        component: RemoveConfirmDialogComponent,
        providers: [ConfirmationService],
        mocks: [ConfirmationService]
    });

    beforeEach(() => {
        spectator = createComponent();
        mockConfirmationService = spectator.inject(ConfirmationService);
        jest.spyOn(mockConfirmationService, 'confirm').mockImplementation(({ accept }) => accept());
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });

    it('should emit confirm event', () => {
        const confirmMock = jest.spyOn(spectator.component, 'confirm');
        const deleteButton = spectator.query(byTestId('btn-remove-item'));
        spectator.dispatchMouseEvent(deleteButton, 'onClick');

        expect(confirmMock).toHaveBeenCalled();
    });
});
