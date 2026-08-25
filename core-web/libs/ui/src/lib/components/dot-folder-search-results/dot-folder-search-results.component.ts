import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';

import { LOAD_MORE_NODE_TYPE, TreeNodeItem } from '@dotcms/dotcms-models';

import { formatFolderSearchPath } from './folder-search-path.utils';

import { DotMessagePipe } from '../../dot-message/dot-message.pipe';
import { DotFolderNamePipe } from '../../pipes/dot-folder-name/dot-folder-name.pipe';

/**
 * Flat list of folder-search results — the folder name over its full path.
 *
 * Shared by the AssetPicker sidebar and the Site/Folder field, which had built this row inline
 * before it was extracted here. Content Drive is the intended third consumer once it grows a folder
 * search of its own; nothing here is coupled to a particular host.
 *
 * **A list, not a one-level tree.** A recursive result set is flat — there is nothing to expand — so
 * modelling it as a tree would inherit indentation, togglers and `role="tree"` that the design does
 * not show. Keeping it separate also leaves {@link DotFolderTreeComponent} alone, which matters
 * while that component is being changed elsewhere for unrelated folder-tree fixes.
 *
 * **Presentation only.** It does not filter, page, or decide when there is nothing to show:
 *
 * - *Scoping* is the caller's (and the server's) job — results arrive already scoped to one site.
 * - *Paging* stays with each consumer, because they disagree: the Site/Folder field pages its
 *   results, while the AssetPicker caps at one page — a "load more" there would page a
 *   non-recursive query and quietly return different folders. Supply `loadMoreLabelKey` to get the
 *   row; omit it and there is none.
 * - *The empty state* belongs to the consumer too. An empty `results` renders nothing at all, so
 *   each host keeps the empty state it already had.
 */
@Component({
    selector: 'dot-folder-search-results',
    templateUrl: './dot-folder-search-results.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [DotMessagePipe, DotFolderNamePipe],
    host: { class: 'block min-w-0 overflow-y-auto' }
})
export class DotFolderSearchResultsComponent {
    /**
     * The matches to render, already scoped and ordered by the caller.
     *
     * May carry a trailing "load more" sentinel, which is rendered as the paging row rather than as
     * a result.
     *
     * @alias results
     */
    readonly $results = input<TreeNodeItem[]>([], { alias: 'results' });

    /**
     * Key of the selected row, or `null`.
     *
     * Selection is by **key**, not by object reference: consumers re-publish cloned result arrays
     * when a page is appended, and a reference-based match would silently lose the highlight.
     *
     * @alias selectedKey
     */
    readonly $selectedKey = input<string | null>(null, { alias: 'selectedKey' });

    /** Shows the loading affordance without blanking the rows already on screen. @alias loading */
    readonly $loading = input(false, { alias: 'loading' });

    /**
     * i18n key for the paging row. Empty (the default) renders no paging row at all — which is how
     * a consumer that caps its results opts out.
     *
     * @alias loadMoreLabelKey
     */
    readonly $loadMoreLabelKey = input('', { alias: 'loadMoreLabelKey' });

    /** @alias listTestId */
    readonly $listTestId = input('dot-folder-search-results', { alias: 'listTestId' });

    /** @alias rowTestId */
    readonly $rowTestId = input('folder-search-result', { alias: 'rowTestId' });

    /** A row was activated. Never emitted for the paging row. */
    readonly resultSelect = output<TreeNodeItem>();

    /** The paging row was activated. Only reachable when `loadMoreLabelKey` is set. */
    readonly loadMore = output<TreeNodeItem>();

    /** Results with the sentinel taken out, so it is never rendered or counted as a match. */
    protected readonly $rows = computed(() =>
        this.$results().filter((node) => node.data?.type !== LOAD_MORE_NODE_TYPE)
    );

    /**
     * The sentinel, but only when the consumer asked for a paging row. Without the label key there
     * is nothing to render it with, and showing an unlabelled row would be worse than none.
     */
    protected readonly $loadMoreNode = computed<TreeNodeItem | null>(() =>
        this.$loadMoreLabelKey()
            ? (this.$results().find((node) => node.data?.type === LOAD_MORE_NODE_TYPE) ?? null)
            : null
    );

    protected readonly formatPath = formatFolderSearchPath;

    protected isSelected(node: TreeNodeItem): boolean {
        const key = this.$selectedKey();

        return key !== null && node.key === key;
    }
}
