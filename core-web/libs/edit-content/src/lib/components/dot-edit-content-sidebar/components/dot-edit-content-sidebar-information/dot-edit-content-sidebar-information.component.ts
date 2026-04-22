import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, input } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';

import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';
import { SkeletonModule } from 'primeng/skeleton';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSContentlet, DotCMSContentType } from '@dotcms/dotcms-models';
import { DotMessagePipe, DotRelativeDatePipe } from '@dotcms/ui';

import { DotEditContentSidebarReferencesDialogComponent, DotReferencesDialogData } from './dot-edit-content-sidebar-references-dialog/dot-edit-content-sidebar-references-dialog.component';

import { ContentletStatusTagPipe } from '../../../../pipes/contentlet-status-tag.pipe';
import { DotNameFormatPipe } from '../../../../pipes/name-format.pipe';

interface ContentSidebarInformation {
    contentlet: DotCMSContentlet;
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
        TagModule,
        ContentletStatusTagPipe,
        DotRelativeDatePipe,
        DotMessagePipe,
        DotNameFormatPipe,
        DatePipe
    ],
    templateUrl: './dot-edit-content-sidebar-information.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        class: 'flex flex-col gap-2'
    }
})
export class DotEditContentSidebarInformationComponent {
    readonly #dotMessageService = inject(DotMessageService);
    readonly #dialogService = inject(DialogService);
    readonly #destroyRef = inject(DestroyRef);

    #referencesDialogRef: DynamicDialogRef | undefined;

    $data = input.required<ContentSidebarInformation>({ alias: 'data' });

    $jsonUrl = computed(() => `/api/v1/content/${this.$data().contentlet.identifier}`);

    $createdTooltipMessage = computed(() => {
        const { contentlet } = this.$data();

        return !contentlet?.creationDate
            ? this.#dotMessageService.get('edit.content.sidebar.information.no.created.yet')
            : null;
    });

    $hasReferences = computed(() => {
        const count = this.$data().referencesPageCount;
        return count && count !== '0';
    });

    constructor() {
        this.#destroyRef.onDestroy(() => this.#referencesDialogRef?.close());
    }

    openReferencesDialog(): void {
        if (this.#referencesDialogRef) return;

        const identifier = this.$data().contentlet?.identifier;
        if (!identifier) return;

        this.#referencesDialogRef = this.#dialogService.open(
            DotEditContentSidebarReferencesDialogComponent,
            {
                header: this.#dotMessageService.get('edit.content.sidebar.references.dialog.title'),
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

        this.#referencesDialogRef.onClose
            .pipe(takeUntilDestroyed(this.#destroyRef))
            .subscribe(() => {
                this.#referencesDialogRef = undefined;
            });
    }
}
