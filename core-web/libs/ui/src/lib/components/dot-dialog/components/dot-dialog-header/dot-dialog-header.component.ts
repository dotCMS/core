import { booleanAttribute, ChangeDetectionStrategy, Component, input, output } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';

/**
 * Header slot of {@link DotDialogComponent}: the title on the left and the window controls on the
 * right.
 *
 * Opinionated on purpose. Title typography, icon size and the spacing of the control cluster are
 * exactly what drifts when every dialog rolls its own header, so the shell owns them. What is
 * genuinely per-dialog — a full-screen toggle, a status badge — is projected through
 * `[dialogHeaderActions]`, which lands to the left of the close button.
 */
@Component({
    selector: 'dot-dialog-header',
    templateUrl: './dot-dialog-header.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [ButtonModule, TooltipModule],
    host: {
        class: 'flex flex-none items-center justify-between gap-2 border-b border-gray-200 px-4 py-3'
    }
})
export class DotDialogHeaderComponent {
    /**
     * Dialog title, already translated by the consumer.
     *
     * @alias title
     */
    readonly $title = input('', { alias: 'title' });

    /**
     * Whether the shell renders the close (✕) button. Turn it off for dialogs that must be resolved
     * through their footer actions.
     *
     * @alias closable
     */
    readonly $closable = input(true, { alias: 'closable', transform: booleanAttribute });

    /**
     * Accessible name and tooltip for the close button, already translated — the shell takes the
     * resolved string rather than an i18n key so it needs no message service of its own.
     *
     * @alias closeLabel
     */
    readonly $closeLabel = input('', { alias: 'closeLabel' });

    /** Emitted when the user clicks the close (✕) button. */
    readonly close = output<void>();
}
