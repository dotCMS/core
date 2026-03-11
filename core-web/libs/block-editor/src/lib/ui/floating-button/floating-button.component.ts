import { NgClass } from '@angular/common';
import { Component, computed, input, output } from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { FileStatus } from '@dotcms/data-access';

/**
 * Standalone Angular component that renders the "Import to dotCMS" floating button.
 *
 * Displays upload progress status and transitions through three visual states:
 * - **default** -- clickable button with the current status label
 * - **loading** -- spinner shown while the upload is in progress
 * - **completed** -- disabled button with a check icon
 * - **error** -- inline alert with a link to dotCMS support
 *
 * All inputs are pushed by the plugin view via `ComponentRef.setInput()`.
 */
@Component({
    selector: 'dot-floating-button',
    templateUrl: './floating-button.component.html',
    styleUrls: ['./floating-button.component.css'],
    standalone: true,
    imports: [ButtonModule, NgClass]
})
export class FloatingButtonComponent {
    /** Current status label displayed on the button (e.g., `'Import to dotCMS'`, `'Uploading'`). */
    readonly label = input('');

    /** Whether an upload is currently in progress. Shows a spinner when `true`. */
    readonly isLoading = input(false);

    /** Emits when the user clicks the import button. */
    readonly byClick = output<void>();

    readonly status = FileStatus;

    /** Title-cased version of the label for display. */
    readonly title = computed(() => {
        const l = this.label();
        if (!l) return '';

        return l[0].toUpperCase() + l.substring(1).toLowerCase();
    });

    /** Whether the upload has completed successfully. */
    readonly isCompleted = computed(() => this.label() === this.status.COMPLETED);
}
