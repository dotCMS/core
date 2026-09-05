import { ChangeDetectionStrategy, Component, inject } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DynamicDialogRef } from 'primeng/dynamicdialog';

import { ExistingContentStore } from '@dotcms/edit-content';
import { DotMessagePipe } from '@dotcms/ui';

/**
 * Footer for the "select existing content" dialog when reused as a Content Drive relationship
 * FILTER. Unlike the edit-content footer, Apply is enabled at zero selections — clearing the
 * selection is a valid filter state ("not filtering by this field").
 */
@Component({
    selector: 'dot-content-drive-relationship-footer',
    imports: [ButtonModule, DotMessagePipe],
    templateUrl: './dot-content-drive-relationship-footer.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotContentDriveRelationshipFooterComponent {
    protected readonly store = inject(ExistingContentStore);
    readonly #dialogRef = inject(DynamicDialogRef);

    /** Apply the current selection (possibly empty) — the dialog closes with the contentlets. */
    apply(): void {
        this.#dialogRef.close(this.store.currentItems());
    }

    cancel(): void {
        this.#dialogRef.close();
    }
}
