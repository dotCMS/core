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

import { DotMessagePipe } from '@dotcms/ui';

import { DotContentReference } from '../../../../../models/dot-edit-content.model';
import { DotEditContentService } from '../../../../../services/dot-edit-content.service';

export interface DotReferencesDialogData {
    identifier: string;
}

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

    $references = signal<DotContentReference[]>([]);
    $loading = signal(true);

    readonly $rows = signal(10);
    readonly $rowsPerPageOptions = [5, 10, 25, 50];

    ngOnInit(): void {
        const { identifier } = this.#dialogConfig.data;

        this.#service
            .getContentletReferences(identifier)
            .pipe(takeUntilDestroyed(this.#destroyRef))
            .subscribe({
                next: (refs) => {
                    this.$references.set(refs);
                    this.$loading.set(false);
                },
                error: () => this.$loading.set(false)
            });
    }

    close(): void {
        this.#dialogRef.close();
    }
}
