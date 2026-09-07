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
import { ContextMenu } from 'primeng/contextmenu';
import { Tree, TreeModule } from 'primeng/tree';
import type {
    TreeNodeCollapseEvent,
    TreeNodeContextMenuSelectEvent,
    TreeNodeExpandEvent,
    TreeNodeSelectEvent
} from 'primeng/types/tree';

import { DotMessagePipe } from '../../dot-message/dot-message.pipe';
import { DotFolderNamePipe } from '../../pipes/dot-folder-name/dot-folder-name.pipe';
import {
    DotTruncatedLabelComponent,
    TRUNCATED_LABEL_ATTR
} from '../dot-truncated-label/dot-truncated-label.component';

/**
 * Presentational folder tree shell shared across Content Drive, Browser Selector,
 * and Host Folder Field. Owns the PrimeNG `p-tree` chrome; consumers own data,
 * empty-loading UX, drag-and-drop, and overlay chrome.
 */
@Component({
    selector: 'dot-folder-tree',
    imports: [
        TreeModule,
        DotFolderNamePipe,
        DotMessagePipe,
        NgTemplateOutlet,
        DotTruncatedLabelComponent
    ],
    templateUrl: './dot-folder-tree.component.html',
    styleUrls: ['./dot-folder-tree.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        '[class]': 'hostClasses()',
        '(focusin)': 'onRowFocus($event)',
        '(focusout)': 'onRowBlur($event)'
    }
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
     * When true, renders a state-reflecting folder icon ahead of each folder row's label:
     * a closed folder when the row is collapsed, an open one when it is expanded.
     *
     * Off by default — consumers opt in. `DotRolesTreeComponent` renders a non-folder
     * hierarchy through this component and draws its own icons, so a default-on rule would
     * stamp a folder glyph next to them.
     *
     * A row that declares an icon of its own (`icon` / `expandedIcon` / `collapsedIcon`) is
     * left alone: PrimeNG already draws that one, and a second would double it. This is how a
     * site row keeps its globe.
     */
    $showFolderIcons = input(false, { alias: 'showFolderIcons' });

    /**
     * `data-testid` applied to the underlying `p-tree`.
     */
    $treeTestId = input('dot-folder-tree', { alias: 'treeTestId' });

    /**
     * `data-testid` applied to the load-more button.
     */
    $loadMoreTestId = input('tree-load-more', { alias: 'loadMoreTestId' });

    /**
     * Extra CSS classes applied to the host for sizing/layout.
     */
    $styleClass = input('w-full h-full', { alias: 'styleClass' });

    /**
     * Optional right-click context menu bound to the underlying `<p-tree>`.
     * When provided, PrimeNG opens it on node right-click and emits the
     * clicked node through `onNodeContextMenuSelect`. Consumers own the
     * menu template and its `MenuItem[]` model.
     */
    $contextMenu = input<ContextMenu | null>(null, { alias: 'contextMenu' });

    onNodeSelect = output<TreeNodeSelectEvent>();
    onNodeExpand = output<TreeNodeExpandEvent>();
    onNodeCollapse = output<TreeNodeCollapseEvent>();
    onNodeContextMenuSelect = output<TreeNodeContextMenuSelectEvent>();
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

    /**
     * Host layout classes (consumer `styleClass` + defaults).
     * Node icons come from `showFolderIcons` (state-driven, drawn here) or from
     * TreeNode.icon / expandedIcon / collapsedIcon (fixed, drawn by PrimeNG) — never the toggler.
     */
    readonly hostClasses = computed(() => `block min-h-0 h-full w-full ${this.$styleClass()}`);

    /**
     * Whether this component draws the folder icon for a given row.
     *
     * A plain template predicate on purpose — the icon is a function of `node.expanded` evaluated
     * at render time, with no per-node signal, computed or stored state behind it. It also never
     * writes to the node: several consumers pass NgRx signal-store state, which is frozen in dev
     * builds.
     */
    protected showsFolderIcon(node: TreeNode): boolean {
        return this.$showFolderIcons() && !node.icon && !node.expandedIcon && !node.collapsedIcon;
    }

    /**
     * Opens the label's overflow tooltip when its row takes keyboard focus.
     *
     * PrimeNG puts the tabindex on the `treeitem` and binds a tooltip's focus listeners to the
     * tooltip's own host element, so focus never reaches the label: without this, the tooltip
     * would only ever open for pointer users. Forwarding the row's focus as a pointer enter on
     * the label reuses PrimeNG's own activation path, which keeps the ellipsis gate, the delay
     * and the dismissal in one place rather than duplicating any of them here. The label is not
     * made focusable instead, because the tree navigates with arrow keys and manages `tabIndex`
     * on the row itself — a focusable label would add a second tab stop to every row.
     */
    protected onRowFocus(event: FocusEvent): void {
        this.#labelOfRow(event)?.dispatchEvent(new MouseEvent('mouseenter'));
    }

    protected onRowBlur(event: FocusEvent): void {
        this.#labelOfRow(event)?.dispatchEvent(new MouseEvent('mouseleave'));
    }

    #labelOfRow(event: FocusEvent): HTMLElement | null {
        const target = event.target as HTMLElement | null;

        // The row itself, never a control inside it. PrimeNG puts the tabindex on the `treeitem`,
        // so that is what arrow-key navigation focuses; a focusable element *within* a row — the
        // Roles panel's add-child button sits right beside the name — is a different target, and
        // popping the row's name tooltip while the user is on that button would be wrong.
        if (!target?.matches('[role="treeitem"]')) {
            return null;
        }

        // First match is the row's own label: PrimeNG renders the node's content before the
        // container holding its children.
        return target.querySelector<HTMLElement>(`[${TRUNCATED_LABEL_ATTR}]`) ?? null;
    }

    protected onLoadMoreClick(event: Event, node: TreeNode): void {
        event.stopPropagation();
        this.loadMore.emit(node);
    }

    protected loadMoreLabel(node: TreeNode): string {
        return node.label || this.$loadMoreLabelKey();
    }
}
