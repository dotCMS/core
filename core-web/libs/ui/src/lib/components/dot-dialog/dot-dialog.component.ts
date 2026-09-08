import {
    afterNextRender,
    ChangeDetectionStrategy,
    Component,
    contentChild,
    isDevMode
} from '@angular/core';

import { DotDialogContentComponent } from './components/dot-dialog-content/dot-dialog-content.component';

/**
 * Layout shell for dialogs that render their own chrome instead of PrimeNG's
 * (`DialogService.open(..., { showHeader: false })`): a fixed header, a body that takes the
 * remaining space, and a footer pinned to the bottom.
 *
 * The shell owns the sizing (`h-full`, `min-h-0`, `overflow-hidden`) and the chrome tokens
 * (dividers, padding), so every modal using it looks the same. What goes inside each slot is the
 * consumer's business.
 *
 * Slot order is fixed by this component's template, not by the order the consumer writes the
 * children in.
 *
 * @example
 * ```html
 * <dot-dialog>
 *     <dot-dialog-header [title]="$title()" (close)="cancel()">
 *         <dot-my-fullscreen-toggle dialogHeaderActions />
 *     </dot-dialog-header>
 *
 *     <dot-dialog-content>...</dot-dialog-content>
 *
 *     <dot-dialog-footer>
 *         <p-button label="Cancel" [text]="true" (click)="cancel()" />
 *         <p-button label="Accept" (click)="confirm()" />
 *     </dot-dialog-footer>
 * </dot-dialog>
 * ```
 *
 * There is deliberately no default `<ng-content />`: a child that matches none of the three slots
 * is dropped silently. Overlays and hidden inputs (`<p-toast>`, `<p-popover>`, `<p-confirmDialog>`,
 * `<input hidden>`) must therefore stay **siblings** of `<dot-dialog>`, not children of it.
 */
@Component({
    selector: 'dot-dialog',
    templateUrl: './dot-dialog.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'flex h-full min-h-0 flex-col overflow-hidden' }
})
export class DotDialogComponent {
    // Not `#private`: Angular rejects signal queries on ES-private members.
    protected readonly $content = contentChild(DotDialogContentComponent);

    constructor() {
        // A missing body is the one way to use the shell that renders nothing and reports nothing,
        // usually because the markup was projected without the wrapper. Say so in dev.
        if (isDevMode()) {
            afterNextRender(() => {
                if (!this.$content()) {
                    console.warn(
                        '<dot-dialog> has no <dot-dialog-content>: children that match no slot are not rendered.'
                    );
                }
            });
        }
    }
}
