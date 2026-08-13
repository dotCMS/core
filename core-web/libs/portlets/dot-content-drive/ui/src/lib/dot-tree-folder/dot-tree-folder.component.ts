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

import { ALL_FOLDER } from '../shared/constants';
import {
    DotFolderTreeNodeData,
    DotFolderTreeNodeItem,
    DotContentDriveUploadFiles,
    DotContentDriveMoveItems,
    DotContentDriveTreeRightClick
} from '../shared/models';

/**
 * Content Drive folder tree wrapper: owns drag-and-drop, ALL_FOLDER labeling,
 * and pulse empty-loading UX around the shared {@link DotFolderTreeComponent}.
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

    protected readonly ALL_FOLDER_KEY = ALL_FOLDER.key;

    protected readonly treePt = {
        root: { class: 'w-full h-full border-none overflow-y-auto' },
        nodeLabel: { class: 'overflow-hidden text-ellipsis whitespace-nowrap' }
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
        // sentinels are not folders (and render a different template, so they should not reach
        // here at all) and neither is the synthetic "All folders" root. Both keep the browser menu
        // rather than having it swallowed for nothing.
        if (
            !isTreeNodeContentData(data) ||
            label?.getAttribute('data-node-key') === this.ALL_FOLDER_KEY
        ) {
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
    onDragEnter(event: DragEvent & { fromElement?: HTMLElement }) {
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

        const files = event.dataTransfer?.files ?? undefined;

        if (files?.length) {
            this.uploadFiles.emit({ files, targetFolder });
        } else {
            this.moveItems.emit({ targetFolder });
        }
    }
}
