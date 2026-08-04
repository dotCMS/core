import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';

import { TableModule } from 'primeng/table';

import { DotCMSContentlet } from '@dotcms/dotcms-models';
import {
    DotContentletStatusBadgeComponent,
    DotContentThumbnailComponent,
    DotMessagePipe
} from '@dotcms/ui';

/**
 * Rows per page in the preview table.
 *
 * Matches the grid's own `MIN_ROWS_PER_PAGE`, so a selection that fit on one page of the grid also
 * fits on one page here.
 */
export const PREVIEW_ROWS_PER_PAGE = 20;

/**
 * The contentlets a workflow action is about to run on, as a checkable table.
 *
 * Purely presentational: the parent owns the included set and the disabled state, so this component
 * holds no state of its own and injects nothing. Columns are deliberately limited to what is needed
 * to recognise a row — title, publish status and content type — rather than mirroring the grid.
 *
 * Rows are keyed on `inode`, **not** `identifier` as the main grid does. Language variants of one
 * contentlet share an identifier but have distinct inodes, and inodes are what gets fired; keying on
 * identifier would collapse two variants into a single selection entry.
 */
@Component({
    selector: 'dot-content-drive-action-preview',
    imports: [
        TableModule,
        DotContentletStatusBadgeComponent,
        DotContentThumbnailComponent,
        DotMessagePipe
    ],
    templateUrl: './dot-content-drive-action-preview.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'block' }
})
export class DotContentDriveActionPreviewComponent {
    /** Every contentlet in the selection — including the ones the user has unchecked. */
    readonly items = input.required<DotCMSContentlet[]>();
    /** The currently included subset; the checked rows. */
    readonly selection = input<DotCMSContentlet[]>([]);
    /** Freezes every checkbox, used while an action is in flight. */
    readonly disabled = input<boolean>(false);

    readonly selectionChange = output<DotCMSContentlet[]>();

    protected readonly ROWS_PER_PAGE = PREVIEW_ROWS_PER_PAGE;

    /**
     * Paginate only once the selection outgrows a page. Below that the paginator is dead weight on
     * what is meant to be a quick confirmation step.
     */
    protected readonly $paginate = computed(() => this.items().length > PREVIEW_ROWS_PER_PAGE);

    protected onSelectionChange(selection: DotCMSContentlet[]): void {
        this.selectionChange.emit(selection);
    }
}
