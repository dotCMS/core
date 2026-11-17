import { Component } from '@angular/core';

/**
 * Icons for PrimeNG's own maximize button on the Edit/Create User dialog.
 *
 * `DynamicDialogConfig.templates` takes component types, not inputs, so each
 * state gets its own tiny component. PrimeNG swaps between them itself:
 * `maximizeicon` while windowed, `minimizeicon` while full screen.
 */
@Component({
    selector: 'dot-users-dialog-maximize-icon',
    templateUrl: './dot-users-dialog-fullscreen-icon.component.html'
})
export class DotUsersDialogMaximizeIconComponent {
    /** Material Symbol ligature shown while the dialog is windowed. */
    protected readonly icon = 'open_in_full';
}

/** Icon PrimeNG shows on the maximize button while the dialog is full screen. */
@Component({
    selector: 'dot-users-dialog-minimize-icon',
    templateUrl: './dot-users-dialog-fullscreen-icon.component.html'
})
export class DotUsersDialogMinimizeIconComponent {
    /** Material Symbol ligature shown while the dialog is full screen. */
    protected readonly icon = 'close_fullscreen';
}
