import { DatePipe } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    DestroyRef,
    computed,
    inject,
    input
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';

import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';
import { SkeletonModule } from 'primeng/skeleton';
import { TooltipModule } from 'primeng/tooltip';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSContentlet, DotCMSContentType } from '@dotcms/dotcms-models';
import { DotContentletStatusChipComponent, DotMessagePipe, DotRelativeDatePipe } from '@dotcms/ui';

import { DotEditContentSidebarReferencesDialogComponent } from './dot-edit-content-sidebar-references-dialog/dot-edit-content-sidebar-references-dialog.component';

import { DotReferencesDialogData } from '../../../../models/dot-edit-content.model';
import { DotNameFormatPipe } from '../../../../pipes/name-format.pipe';

interface ContentSidebarInformation {
    contentlet: DotCMSContentlet | null;
    contentType: DotCMSContentType;
    loading: boolean;
    referencesPageCount: string;
}

/**
 * Component that displays the information of a contentlet in the sidebar.
 */
@Component({
    selector: 'dot-edit-content-sidebar-information',
    imports: [
        RouterLink,
        TooltipModule,
        SkeletonModule,
        DotContentletStatusChipComponent,
        DotRelativeDatePipe,
        DotMessagePipe,
        DotNameFormatPipe,
        DatePipe
    ],
    templateUrl: './dot-edit-content-sidebar-information.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [DialogService],
    host: {
        class: 'flex flex-col gap-2'
    }
})
export class DotEditContentSidebarInformationComponent {
    readonly #dotMessageService = inject(DotMessageService);
    readonly #dialogService = inject(DialogService);
    readonly #destroyRef = inject(DestroyRef);

    #referencesDialogRef: DynamicDialogRef | undefined;

    /** The sidebar data including the contentlet, content type, loading state, and references count. */
    readonly $data = input.required<ContentSidebarInformation>({ alias: 'data' });

    /** URL to fetch the contentlet as JSON via the REST API. */
    readonly $jsonUrl = computed(
        () => `/api/v1/content/${this.$data().contentlet?.identifier ?? ''}`
    );

    /** Tooltip message shown when the contentlet has no creation date yet. */
    readonly $createdTooltipMessage = computed(() => {
        const { contentlet } = this.$data();

        return !contentlet?.creationDate
            ? this.#dotMessageService.get('edit.content.sidebar.information.no.created.yet')
            : null;
    });

    /** Whether the contentlet has at least one page reference. Controls the clickable card variant. */
    readonly $hasReferences = computed(() => {
        const count = this.$data().referencesPageCount;
        return !!count && count !== '0';
    });

    constructor() {
        this.#destroyRef.onDestroy(() => this.#referencesDialogRef?.close());
    }

    /** Opens the references dialog showing all pages that include this contentlet. */
    openReferencesDialog(): void {
        if (this.#referencesDialogRef) return;

        const identifier = this.$data().contentlet?.identifier;
        if (!identifier) return;

        this.#referencesDialogRef = this.#dialogService.open(
            DotEditContentSidebarReferencesDialogComponent,
            {
                header: this.#dotMessageService.get(
                    'edit.content.sidebar.references.dialog.title',
                    this.$data().contentlet.title
                ),
                width: 'min(92vw, 60rem)',
                contentStyle: { padding: '0', overflow: 'auto' },
                data: { identifier } satisfies DotReferencesDialogData,
                modal: true,
                appendTo: 'body',
                closeOnEscape: true,
                closable: true,
                draggable: false,
                resizable: false,
                position: 'center'
            }
        );

        this.#referencesDialogRef.onClose.pipe(takeUntilDestroyed(this.#destroyRef)).subscribe({
            next: () => {
                this.#referencesDialogRef = undefined;
            },
            error: () => {
                this.#referencesDialogRef = undefined;
            }
        });
    }
}
