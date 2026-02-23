import { Spectator, createComponentFactory } from '@ngneat/spectator';
import { byTestId } from '@ngneat/spectator/jest';

jest.mock('primeng/dynamicdialog', () => ({
    DynamicDialogRef: class DynamicDialogRef {},
    DynamicDialogConfig: class DynamicDialogConfig {}
}));

import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';
import { DotLanguage } from '@dotcms/dotcms-models';
import { DotMessagePipe, MockDotMessageService } from '@dotcms/utils-testing';

import { DotLocaleConfirmationDialogComponent } from './DotLocaleConfirmationDialog.component';

const messageServiceMock = new MockDotMessageService({
    Cancel: 'Cancel'
});

/** Mock ref for assertions; component injects DynamicDialogRef from primeng */
interface DialogRefMock {
    close: jest.Mock;
}

describe('DotLocaleConfirmationDialogComponent', () => {
    let spectator: Spectator<DotLocaleConfirmationDialogComponent>;
    const createComponent = createComponentFactory({
        component: DotLocaleConfirmationDialogComponent,
        imports: [DotMessagePipe],
        providers: [
            {
                provide: DynamicDialogRef,
                useValue: { close: jest.fn() }
            },
            {
                provide: DynamicDialogConfig,
                useValue: {
                    data: {
                        acceptLabel: '',
                        icon: '',
                        ISOCode: '',
                        locale: {} as DotLanguage,
                        message: ''
                    }
                }
            },
            {
                provide: DotMessageService,
                useValue: messageServiceMock
            }
        ]
    });
    beforeEach(() => (spectator = createComponent()));

    it('should disable the confirm button if input value is different from ISOCode', () => {
        spectator.component.data.ISOCode = 'en-us';
        spectator.detectChanges();

        const inputElement = spectator.query<HTMLInputElement>(byTestId('input'));

        spectator.typeInElement('fr-ca', inputElement);

        const acceptButton = spectator.query<HTMLButtonElement>(byTestId('confirm-button'));
        expect(acceptButton.disabled).toEqual(true);
    });

    it('should enable the confirm button if input value is same as ISOCode', () => {
        const ref = spectator.component.ref as DialogRefMock;
        jest.spyOn(ref, 'close');
        spectator.component.data.ISOCode = 'en-us';
        spectator.detectChanges();

        const inputElement = spectator.query<HTMLInputElement>(byTestId('input'));
        spectator.typeInElement('en-us', inputElement);

        const buttonElement = spectator.query<HTMLButtonElement>(byTestId('confirm-button'));

        spectator.click(buttonElement);

        expect(buttonElement.disabled).toEqual(false);
        expect(ref.close).toHaveBeenCalledWith(true);
    });

    it('should close the dialog without confirmation when cancel button is clicked', () => {
        const ref = spectator.component.ref as DialogRefMock;
        jest.spyOn(ref, 'close');
        spectator.detectChanges();

        const cancelButton = spectator.query<HTMLButtonElement>(byTestId('cancel-button'));

        spectator.click(cancelButton);

        expect(ref.close).toHaveBeenCalledWith(false);
    });
});
