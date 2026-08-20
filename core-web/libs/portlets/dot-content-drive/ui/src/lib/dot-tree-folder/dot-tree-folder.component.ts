import { JsonPipe } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    ElementRef,
    HostListener,
    inject,
    input,
    output,
    signal
} from '@angular/core';

import { TreeNode } from 'primeng/api';
import { TreeNodeExpandEvent, TreeNodeCollapseEvent } from 'primeng/types/tree';

import { isTreeNodeContentData } from '@dotcms/dotcms-models';
import { DotFolderTreeComponent, DotFolderNamePipe, DotMessagePipe } from '@dotcms/ui';

import {
    DotFolderTreeNodeData,
    DotFolderTreeNodeItem,
    DotContentDriveUploadFiles,
    DotContentDriveMoveItems,
    DotContentDriveTreeRightClick
} from '../shared/models';

/**
 * Content Drive folder tree wrapper: owns drag-and-drop, site-row styling, and pulse
 * empty-loading UX around the shared {@link DotFolderTreeComponent}.
 */
@Component({
    selector: 'dot-tree-folder',
    imports: [DotFolderTreeComponent, DotFolderNamePipe, DotMessagePipe, JsonPipe],
    templateUrl: './dot-tree-folder.component.html',
    styleUrls: ['./dot-tree-folder.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'w-full h-full min-h-0 block' }
})
export class DotTreeFolderComponent {
    $folders = input.required<TreeNode[]>({ alias: 'folders' });

    $loading = input.required<boolean>({ alias: 'loading' });

    $selectedNode = input<TreeNode | null>(null, { alias: 'selectedNode' });

    onNodeExpand = output<TreeNodeExpandEvent>();
    onNodeSelect = output<TreeNodeExpandEvent>();
    onNodeCollapse = output<TreeNodeCollapseEvent>();
    loadMore = output<DotFolderTreeNodeItem>();
    uploadFiles = output<DotContentDriveUploadFiles>();
    moveItems = output<DotContentDriveMoveItems>();
    /** Right-click on a folder node — drives the shared folder context menu, as the table's rows do. */
    rightClick = output<DotContentDriveTreeRightClick>();

    readonly elementRef = inject(ElementRef);

    readonly $activeDropNode = signal<DotFolderTreeNodeData | null>(null);

    protected readonly treePt = {
        root: { class: 'w-full h-full border-none overflow-y-auto' },
        nodeLabel: { class: 'overflow-hidden text-ellipsis whitespace-nowrap' },
        /**
         * The node icon (a globe on the site row, a folder on the rest) has never had dotCMS styling.
         * `dotcms-theme/components/_tree.scss` is meant to own it, but that theme is not in the build
         * at all — `dotcms-scss/angular/styles.scss` has its `@import "dotcms-theme/theme"` commented
         * out — and its selectors predate PrimeNG 21 anyway (`.p-treenode-icon`, now
         * `.p-tree-node-icon`). So the icon was left inheriting the 14px root font size, which made it
         * the largest thing in the row, and relying on PrimeNG's 4px node gap.
         *
         * Like the toggler chevron, the icon owns its own centring: `flex size-4 items-center
         * justify-center` gives the glyph a fixed square box and centres it inside that box, so its
         * position stops depending on the icon font's baseline. A baseline-aligned glyph shifts as the
         * em box changes, which is why the alignment previously got worse as the icon got smaller.
         *
         * The `!` modifiers are required because PrimeNG is provided without a `cssLayer`, so its CSS
         * is injected unlayered and outranks Tailwind utilities, which live in `@layer utilities`.
         * `mr-1` adds to the node's own 4px gap for the 8px the theme asked for.
         */
        nodeIcon: {
            class: 'flex! size-4 shrink-0 translate-y-px items-center justify-center text-sm! leading-none! mr-1'
        }
    };

    protected onLoadMore(node: TreeNode): void {
        this.loadMore.emit(node as DotFolderTreeNodeItem);
    }

    /**
     * @description Emits a right-click on a real folder node so the consumer can open the shared
     * context menu.
     *
     * Bound on the host and resolved from the row rather than on the label element, so the whole
     * node responds the way the table's rows do: the toggler, the indent gutter and the empty space
     * past a short folder name all open the menu, instead of only the few characters of text.
     * Resolution mirrors `onDragEnter`/`onDragOver`, which already treat the row as the target.
     *
     * The browser menu is suppressed only for nodes that can actually produce one: the synthetic
     * "All folders" root and "Load more" sentinels are not folders, so they keep the native menu
     * rather than swallowing the event for no reason.
     *
     * @param event - The contextmenu MouseEvent
     */
    @HostListener('contextmenu', ['$event'])
    onContextMenu(event: MouseEvent): void {
        // A node's content wrapper holds its toggler, icon and label but *not* its children list,
        // so the nearest one is always the clicked node's own row. Resolving against the `treeitem`
        // element instead would climb to the parent whenever a row has no label of its own, such as
        // a "Load more" sentinel, and open that parent's menu.
        const label = (event.target as HTMLElement | null)
            ?.closest('.p-tree-node-content')
            ?.querySelector('[data-testid="tree-node-label"]');

        const nodeData = label?.getAttribute('data-json-node');

        if (!nodeData) {
            return;
        }

        const data = JSON.parse(nodeData) as DotFolderTreeNodeData;

        // Only nodes that can actually produce a menu get the native one suppressed. "Load more"
        // sentinels are not folders (and render a different template, so they should not reach here
        // at all), and a row with no path is a site rather than a folder, so it has no folder
        // permissions to build a menu from. Both keep the browser menu rather than having it
        // swallowed for nothing.
        if (!isTreeNodeContentData(data) || !data.path) {
            return;
        }

        event.preventDefault();
        this.rightClick.emit({ event, data });
    }

    /**
     * @description Set the dropzone as active when the drag enters the dropzone
     * @param event - DragEvent
     */
    @HostListener('dragenter', ['$event'])
    onDragEnter(event: DragEvent & { fromElement?: HTMLElement | null }) {
        event.stopPropagation();
        event.preventDefault();
    }

    /**
     * @description Prevent the default behavior to allow drop and not opening the file in the browser
     * @param event - DragEvent
     */
    @HostListener('dragover', ['$event'])
    onDragOver(event: DragEvent) {
        event.stopPropagation();
        event.preventDefault();

        const target = event.target as HTMLElement;

        let activeNodeSpan: HTMLElement | null = null;

        if (target.hasAttribute('data-json-node')) {
            activeNodeSpan = target;
        } else {
            activeNodeSpan = target.querySelector('[data-testid="tree-node-label"]');
        }

        if (!activeNodeSpan) {
            console.warn('Content drive tree folder: No active node span found');
            return;
        }

        const nodeData = activeNodeSpan.getAttribute('data-json-node');

        if (!nodeData) {
            console.warn('Content drive tree folder: No node data found');
            return;
        }

        this.$activeDropNode.set(JSON.parse(nodeData));
    }

    /**
     * @description Set the dropzone as inactive when the drag leaves the dropzone
     * @param event - DragEvent
     */
    @HostListener('dragleave', ['$event'])
    onDragLeave(event: DragEvent) {
        event.preventDefault();

        const relatedTarget = event.relatedTarget as Node;

        if (relatedTarget && this.elementRef.nativeElement.contains(relatedTarget)) {
            return;
        }

        this.$activeDropNode.set(null);
    }

    /**
     * @description Handle drop event
     * @param event - DragEvent
     */
    @HostListener('drop', ['$event'])
    onDrop(event: DragEvent) {
        event.stopPropagation();
        event.preventDefault();

        const targetFolder = this.$activeDropNode();

        this.$activeDropNode.set(null);

        // Both payloads declare a non-null targetFolder, and a drop with no active
        // drop node has no destination to report.
        if (!targetFolder) {
            return;
        }

        const files = event.dataTransfer?.files ?? undefined;

        if (files?.length) {
            this.uploadFiles.emit({ files, targetFolder });
        } else {
            this.moveItems.emit({ targetFolder });
        }
    }
}
