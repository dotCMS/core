import {
    ChangeDetectionStrategy,
    Component,
    DestroyRef,
    OnInit,
    inject,
    signal
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { ButtonModule } from 'primeng/button';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { SkeletonModule } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';

import { DotHttpErrorManagerService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import {
    DotContentReference,
    DotReferencesDialogData
} from '../../../../../models/dot-edit-content.model';
import { DotEditContentService } from '../../../../../services/dot-edit-content.service';

/**
 * Dialog that lists all pages referencing a given contentlet.
 * Opened from the sidebar information panel when the contentlet has at least one page reference.
 */
@Component({
    selector: 'dot-edit-content-sidebar-references-dialog',
    imports: [TableModule, ButtonModule, SkeletonModule, DotMessagePipe],
    templateUrl: './dot-edit-content-sidebar-references-dialog.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEditContentSidebarReferencesDialogComponent implements OnInit {
    readonly #dialogConfig =
        inject<DynamicDialogConfig<DotReferencesDialogData>>(DynamicDialogConfig);
    readonly #dialogRef = inject(DynamicDialogRef);
    readonly #service = inject(DotEditContentService);
    readonly #destroyRef = inject(DestroyRef);
    readonly #errorManager = inject(DotHttpErrorManagerService);

    /** List of page references for the contentlet. Populated after the HTTP call resolves. */
    readonly $references = signal<DotContentReference[]>([]);
    /** Whether the HTTP call is still in flight. Controls skeleton vs. table visibility. */
    readonly $loading = signal(true);

    /** Number of rows shown per page in the references table. */
    readonly $rows = signal(10);
    readonly rowsPerPageOptions = [5, 10, 25, 50] as const;

    ngOnInit(): void {
        const identifier = this.#dialogConfig.data?.identifier;

        if (!identifier) {
            this.close();
            return;
        }

        this.#service
            .getContentletReferences(identifier)
            .pipe(takeUntilDestroyed(this.#destroyRef))
            .subscribe({
                next: (refs) => {
                    this.$references.set(refs);
                    this.$loading.set(false);
                },
                error: (err) => {
                    this.$loading.set(false);
                    this.#errorManager.handle(err);
                }
            });
    }

    close(): void {
        this.#dialogRef.close();
    }
}
