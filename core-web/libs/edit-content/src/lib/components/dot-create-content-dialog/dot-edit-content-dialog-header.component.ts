import { Component, ElementRef, afterNextRender, inject, signal } from '@angular/core';

import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotContentletStatusBadgeComponent } from '@dotcms/ui';

import { DotEditContentDialogTitleStatusService } from './dot-edit-content-dialog-title-status.service';

/**
 * The header of the Edit Content dialog: the title its opener gave the dialog, and the content
 * state chip right after it.
 *
 * Openers pass it as the dialog's `templates.header`. PrimeNG stops printing the `header` text when
 * a header template is given, so the title is rendered here from the same `header` option. The chip
 * comes from the editor inside the dialog (see {@link DotEditContentDialogTitleStatusService}).
 *
 * PrimeNG also stops printing the title's `id`, while the dialog keeps pointing its
 * `aria-labelledby` at it, which would leave the dialog without an accessible name. The header
 * adopts that id for its own title once it is on screen.
 */
@Component({
    selector: 'dot-edit-content-dialog-header',
    templateUrl: './dot-edit-content-dialog-header.component.html',
    imports: [DotContentletStatusBadgeComponent],
    host: { class: 'flex min-w-0 flex-1 items-center' }
})
export class DotEditContentDialogHeaderComponent {
    readonly #config = inject(DynamicDialogConfig);
    readonly #host = inject<ElementRef<HTMLElement>>(ElementRef);

    /** The title the opener gave the dialog. */
    protected readonly title = this.#config.header ?? '';

    /** The chip next to the title; `null` hides it. */
    protected readonly $titleStatus = inject(DotEditContentDialogTitleStatusService).for(
        inject(DynamicDialogRef)
    );

    /** The id the dialog's `aria-labelledby` points at, once the header is inside the dialog. */
    protected readonly $titleId = signal<string | null>(null);

    constructor() {
        afterNextRender(() => {
            const dialog = this.#host.nativeElement.closest('[role="dialog"]');

            this.$titleId.set(dialog?.getAttribute('aria-labelledby') ?? null);
        });
    }
}
