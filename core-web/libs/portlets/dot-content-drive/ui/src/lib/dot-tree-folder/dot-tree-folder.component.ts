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

import { DotFolderTreeComponent, DotFolderNamePipe, DotMessagePipe } from '@dotcms/ui';

import { ALL_FOLDER, LOAD_MORE_NODE_TYPE } from '../shared/constants';
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
     * context menu. The browser menu is suppressed only for nodes that can actually produce one:
     * the synthetic "All folders" root and "Load more" sentinels are not folders, so they keep the
     * native menu rather than swallowing the event for no reason.
     * @param event - The contextmenu MouseEvent
     * @param node - The tree node under the cursor
     */
    protected onContextMenu(event: MouseEvent, node: DotFolderTreeNodeItem): void {
        const data = node?.data;

        if (!data || data.type === LOAD_MORE_NODE_TYPE || node.key === this.ALL_FOLDER_KEY) {
            return;
        }

        event.preventDefault();
        this.rightClick.emit({ event, node });
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
