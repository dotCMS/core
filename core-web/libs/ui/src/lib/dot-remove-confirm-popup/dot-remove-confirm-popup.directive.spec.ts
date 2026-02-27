import { createHostFactory, SpectatorHost } from '@ngneat/spectator/jest';

import { Component } from '@angular/core';

import { ConfirmationService } from 'primeng/api';
import { ConfirmPopupModule } from 'primeng/confirmpopup';

import { DotRemoveConfirmPopupWithEscapeDirective } from './dot-remove-confirm-popup.directive';

@Component({
    selector: 'dot-escape-confirm-popup-host',
    template: `
        <p-confirmpopup dotRemoveConfirmPopupWithEscape></p-confirmpopup>
    `,
    imports: [ConfirmPopupModule, DotRemoveConfirmPopupWithEscapeDirective]
})
class CustomHostComponent {}

describe('DotRemoveConfirmPopupWithEscape', () => {
    let spectator: SpectatorHost<CustomHostComponent>;
    let confirmationService: ConfirmationService;

    const createHost = createHostFactory({
        component: CustomHostComponent,
        providers: [ConfirmationService]
    });

    beforeEach(() => {
        spectator = createHost(`<dot-escape-confirm-popup-host />`);
        spectator.detectChanges();

        confirmationService = spectator.inject(ConfirmationService);
    });

    it('should close the confirmPopup with escape', () => {
        jest.spyOn(confirmationService, 'close');

        const event = new KeyboardEvent('keydown', {
            key: 'Escape',
            code: 'Escape',
            bubbles: true
        });
        document.dispatchEvent(event);

        expect(confirmationService.close).toHaveBeenCalledTimes(1);
    });
});
