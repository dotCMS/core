import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

import { DotCMSContentlet, DotContentDriveItem } from '@dotcms/dotcms-models';
import {
    DotFolderListViewColumnField,
    DotFolderListViewComponent
} from '@dotcms/portlets/content-drive/ui';

/**
 * The contentlets a workflow action is about to run on, as a checkable table.
 *
 * A configuration of the Content Drive grid rather than a table of its own. It used to be a
 * hand-rolled copy that deliberately followed the grid's markup — two near-identical tables that
 * drifted apart every time either was touched, and the copy never grew the grid's per-row lock icon,
 * which is precisely what this screen exists to show.
 *
 * Purely presentational: the parent owns the included set, the disabled state and which rows carry a
 * foreign lock, so this holds no state and injects nothing.
 */
@Component({
    selector: 'dot-content-drive-action-preview',
    imports: [DotFolderListViewComponent],
    templateUrl: './dot-content-drive-action-preview.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    // A shrinkable flex column so the grid's `scrollHeight="flex"` has a bounded height to resolve
    // against: the table fills the dialog and scrolls its own body, keeping the column headers
    // pinned and the paginator out of the scroll region. Without `min-h-0` this grows to fit every
    // row instead.
    host: { class: 'flex min-h-0 flex-1 flex-col' }
})
export class DotContentDriveActionPreviewComponent {
    /** Every contentlet in the selection — including the ones the user has unchecked. */
    readonly items = input.required<DotCMSContentlet[]>();
    /** The currently included subset; the checked rows. */
    readonly selection = input<DotCMSContentlet[]>([]);
    /** Freezes every checkbox, used while an action is in flight. */
    readonly disabled = input<boolean>(false);
    /**
     * Inodes whose lock belongs to another user, marked so the user can drop them before firing.
     * Decided by the parent, which knows the current user's admin role.
     */
    readonly lockedByOthers = input<string[]>([]);

    readonly selectionChange = output<DotCMSContentlet[]>();

    /**
     * Columns the preview keeps, out of the grid's full set: the title (with its thumbnail), the
     * publish status and the content type — enough to recognise a row and no more. Locale,
     * edited-by, last-edited and the actions column are dropped; the dialog is far narrower than the
     * portlet and the full set overflows it.
     */
    protected readonly PREVIEW_COLUMNS: DotFolderListViewColumnField[] = [
        'title',
        'live',
        'contentType'
    ];

    /**
     * The grid speaks `DotContentDriveItem` (contentlets *or* folders); a preview only ever lists
     * contentlets, so the emitted rows are narrowed back on the way out.
     */
    protected onSelectionChange(selection: DotContentDriveItem[]): void {
        this.selectionChange.emit(selection as DotCMSContentlet[]);
    }
}
