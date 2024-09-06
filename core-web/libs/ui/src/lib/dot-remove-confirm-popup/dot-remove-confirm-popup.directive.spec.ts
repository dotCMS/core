import { createHostFactory, SpectatorHost } from '@ngneat/spectator';

import { Component } from '@angular/core';

import { ConfirmationService } from 'primeng/api';
import { ConfirmPopupModule } from 'primeng/confirmpopup';

import { DotRemoveConfirmPopupWithEscapeDirective } from '@dotcms/ui';

@Component({
    selector: 'dot-escape-confirm-popup-host',
    template: `
        <p-confirmPopup dotRemoveConfirmPopupWithEscape></p-confirmPopup>
    `,
    imports: [ConfirmPopupModule]
})
class CustomHostComponent {}

describe('DotRemoveConfirmPopupWithEscape', () => {
    let spectator: SpectatorHost<CustomHostComponent>;
    let confirmationService: ConfirmationService;

    const createHost = createHostFactory({
        component: CustomHostComponent,
        imports: [ConfirmPopupModule, DotRemoveConfirmPopupWithEscapeDirective],
        providers: [ConfirmationService]
    });

    beforeEach(() => {
        spectator = createHost(`<dot-escape-confirm-popup-host />`);
        spectator.detectChanges();

        confirmationService = spectator.inject(ConfirmationService);
    });

    it('should close the confirmPopup with escape', () => {
        spyOn(confirmationService, 'close');
        spectator.dispatchKeyboardEvent(document, 'keydown', 'Escape');
        expect(confirmationService.close).toHaveBeenCalledTimes(1);
    });
});
