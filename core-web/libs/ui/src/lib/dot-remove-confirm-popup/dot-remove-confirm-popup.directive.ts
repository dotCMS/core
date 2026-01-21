import { Directive, HostListener, inject } from '@angular/core';

import { ConfirmationService } from 'primeng/api';

/**
 * Directive that adds to p-confirmPopup the escape functionality.
 *
 * The directive listens for the 'keydown' event on the document element.
 * When the 'Escape' key is pressed, it closes it using the confirmation service.
 */
@Directive({
    selector: 'p-confirmPopup[dotRemoveConfirmPopupWithEscape]'
})
export class DotRemoveConfirmPopupWithEscapeDirective {
    private confirmationService: ConfirmationService = inject(ConfirmationService);

    @HostListener('document:keydown.escape', ['$event'])
    onPressEscape() {
        this.confirmationService.close();
    }
}
