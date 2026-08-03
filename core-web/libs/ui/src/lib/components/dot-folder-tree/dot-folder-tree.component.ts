import { NgTemplateOutlet } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    computed,
    contentChild,
    ElementRef,
    inject,
    input,
    output,
    TemplateRef,
    viewChild
} from '@angular/core';

import type { TreeNode } from 'primeng/api';
import { Tree, TreeModule } from 'primeng/tree';
import type {
    TreeNodeCollapseEvent,
    TreeNodeExpandEvent,
    TreeNodeSelectEvent
} from 'primeng/types/tree';

import { DotMessagePipe } from '../../dot-message/dot-message.pipe';
import { DotFolderNamePipe } from '../../pipes/dot-folder-name/dot-folder-name.pipe';

/**
 * Presentational folder tree shell shared across Content Drive, Browser Selector,
 * and Host Folder Field. Owns the PrimeNG `p-tree` chrome; consumers own data,
 * empty-loading UX, drag-and-drop, and overlay chrome.
 */
@Component({
    selector: 'dot-folder-tree',
    imports: [TreeModule, DotFolderNamePipe, DotMessagePipe, NgTemplateOutlet],
    templateUrl: './dot-folder-tree.component.html',
    styleUrls: ['./dot-folder-tree.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'block min-h-0 h-full w-full' }
})
export class DotFolderTreeComponent {
    /**
     * Tree nodes to render.
     */
    $folders = input.required<TreeNode[]>({ alias: 'folders' });

    /**
     * Tree-level PrimeNG loading icon (e.g. Host Folder partial reload).
     * Does not own consumer empty-state UX (pulse / skeletons / panel spinner).
     */
    $loading = input(false, { alias: 'loading' });

    /**
     * Currently selected node(s). Normalized for PrimeNG based on `selectionMode`.
     */
    $selectedNode = input<TreeNode | TreeNode[] | null>(null, { alias: 'selectedNode' });

    /**
     * PrimeNG selection mode.
     */
    $selectionMode = input<'single' | 'multiple'>('single', { alias: 'selectionMode' });

    /**
     * PrimeNG scrollHeight. Omit / null to leave PrimeNG default.
     */
    $scrollHeight = input<string | null>(null, { alias: 'scrollHeight' });

    /**
     * Whether meta-key is required for multi-selection.
     */
    $metaKeySelection = input(false, { alias: 'metaKeySelection' });

    /**
     * When true, folder icon only on the first root toggler (`first-only`).
     * When false, chevron-only togglers (Browser Selector / Host Folder).
     */
    $showFolderIconOnFirstOnly = input(false, { alias: 'showFolderIconOnFirstOnly' });

    /**
     * PrimeNG pass-through options for tree layout/styling.
     */
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    $pt = input<Record<string, any> | null>(null, { alias: 'pt' });

    /**
     * i18n key used for the load-more button when `node.label` is empty.
     * Empty by default — consumers pass a key or set `node.label`.
     */
    $loadMoreLabelKey = input('', { alias: 'loadMoreLabelKey' });

    /**
     * When true, shows `(remaining)` beside the load-more label (Content Drive).
     */
    $showLoadMoreRemaining = input(false, { alias: 'showLoadMoreRemaining' });

    /**
     * When true, shows a plus-circle icon on the load-more button (Host Folder).
     * Off by default — consumers opt in.
     */
    $showLoadMorePlusIcon = input(false, { alias: 'showLoadMorePlusIcon' });

    /**
     * `data-testid` applied to the underlying `p-tree`.
     */
    $treeTestId = input('dot-folder-tree', { alias: 'treeTestId' });

    /**
     * `data-testid` applied to the load-more button.
     */
    $loadMoreTestId = input('tree-load-more', { alias: 'loadMoreTestId' });

    /**
     * Extra CSS classes applied to `p-tree` in addition to toggler mode classes.
     */
    $styleClass = input('w-full h-full', { alias: 'styleClass' });

    onNodeSelect = output<TreeNodeSelectEvent>();
    onNodeExpand = output<TreeNodeExpandEvent>();
    onNodeCollapse = output<TreeNodeCollapseEvent>();
    loadMore = output<TreeNode>();

    /**
     * Optional projected node label template (`#folderTreeNodeLabel`).
     */
    nodeLabelTemplate = contentChild<TemplateRef<{ $implicit: TreeNode }>>('folderTreeNodeLabel');

    readonly tree = viewChild(Tree);
    readonly elementRef = inject(ElementRef);

    readonly $selection = computed(() => {
        const selected = this.$selectedNode();
        const mode = this.$selectionMode();

        if (mode === 'multiple') {
            if (!selected) {
                return [];
            }

            return Array.isArray(selected) ? selected : [selected];
        }

        if (!selected) {
            return null;
        }

        return Array.isArray(selected) ? (selected[0] ?? null) : selected;
    });

    readonly treeStyleClasses = computed(() => {
        const modeClass = this.$showFolderIconOnFirstOnly() ? 'first-only' : 'chevron-only';

        return `${this.$styleClass()} ${modeClass}`;
    });

    protected onLoadMoreClick(event: Event, node: TreeNode): void {
        event.stopPropagation();
        this.loadMore.emit(node);
    }

    protected loadMoreLabel(node: TreeNode): string {
        return node.label || this.$loadMoreLabelKey();
    }
}
