import { JsonPipe } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    input,
    output,
    computed,
    ElementRef,
    HostListener,
    inject,
    signal,
    InputSignal
} from '@angular/core';

import { TreeNode } from 'primeng/api';
import { TreeModule, TreeNodeExpandEvent, TreeNodeCollapseEvent } from 'primeng/tree';

import { DotMessagePipe, FolderNamePipe } from '@dotcms/ui';

import { TreeNodeData, DotContentDriveUploadFiles } from '../shared/models';

@Component({
    selector: 'dot-tree-folder',
    imports: [TreeModule, FolderNamePipe, DotMessagePipe, JsonPipe],
    templateUrl: './dot-tree-folder.component.html',
    styleUrls: ['./dot-tree-folder.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotTreeFolderComponent {
    /**
     * A signal that emits an array of TreeNode objects representing the folders.
     *
     * @type {InputSignal<TreeNode[]>}
     * @alias folders
     */
    $folders = input.required<TreeNode[]>({ alias: 'folders' });

    /**
     * A boolean signal that indicates the loading state.
     *
     * @type {InputSignal<boolean>}
     */
    $loading = input.required<boolean>({ alias: 'loading' });

    /**
     * A signal that represents the currently selected node in the tree.
     *
     * The transform function is used to convert a single TreeNode or null into an array
     * because PrimeNG's p-tree component expects the selection to be an array of TreeNodes,
     * even when in single selection mode. This allows us to maintain a cleaner API where
     * consumers can pass a single node while internally handling PrimeNG's requirements.
     *
     * @type {InputSignal<TreeNode | TreeNode[] | null, TreeNode | null>}
     */
    $selectedNode = input<TreeNode | TreeNode[] | null, TreeNode | null>(null, {
        alias: 'selectedNode',
        transform: (value: TreeNode | null): TreeNode[] => {
            return value ? [value] : [];
        }
    });

    /**
     * Controls whether the folder icon should be shown only on the first root node's toggler.
     * When false, folder icons will be used for all togglers.
     *
     * @type {InputSignal<boolean>}
     */
    $showFolderIconOnFirstOnly: InputSignal<boolean> = input<boolean>(false, {
        alias: 'showFolderIconOnFirstOnly'
    });

    /**
     * Event emitter for when a tree node is expanded.
     *
     * This event is triggered when a user expands a node in the tree structure.
     * It emits an event of type `TreeNodeExpandEvent`.
     */
    onNodeExpand = output<TreeNodeExpandEvent>();

    /**
     * Event emitter for when a node is selected in the tree.
     *
     * @event onNodeSelect
     * @type {TreeNodeExpandEvent}
     */
    onNodeSelect = output<TreeNodeExpandEvent>();

    /**
     * Event emitter for when a node is collapsed.
     *
     * @event onNodeCollapse
     * @type {TreeNodeCollapseEvent}
     */
    onNodeCollapse = output<TreeNodeCollapseEvent>();

    /**
     * Event emitter for when a file is uploaded.
     *
     * @event uploadFiles
     * @type {DotContentDriveUploadFiles}
     */
    uploadFiles = output<DotContentDriveUploadFiles>();

    readonly elementRef = inject(ElementRef);

    readonly $activeDropNode = signal<TreeNodeData | null>(null);

    /**
     * Computed style classes for the underlying p-tree component.
     */
    readonly treeStyleClasses = computed(
        () => `w-full h-full ${this.$showFolderIconOnFirstOnly() ? 'first-only' : 'folder-all'}`
    );

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

        // First, check if the target itself has the data-json-node attribute
        let activeNodeSpan: HTMLElement | null = null;

        if (target.hasAttribute('data-json-node')) {
            activeNodeSpan = target;
        } else {
            // If not, search for it within the target's children
            activeNodeSpan = target.querySelector('[data-testid="tree-node-label"]');
        }

        if (activeNodeSpan) {
            const nodeData = activeNodeSpan.getAttribute('data-json-node');
            if (nodeData) {
                this.$activeDropNode.set(JSON.parse(nodeData));
            }
        }
    }

    /**
     * @description Set the dropzone as inactive when the drag leaves the dropzone
     * @param event - DragEvent
     */
    @HostListener('dragleave', ['$event'])
    onDragLeave(event: DragEvent) {
        event.preventDefault();

        // Check if the relatedTarget (where the drag is going) is still within the dropzone
        const relatedTarget = event.relatedTarget as Node;

        if (relatedTarget && this.elementRef.nativeElement.contains(relatedTarget)) {
            return; // Still within the dropzone, don't deactivate
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

        const targetFolderId = this.$activeDropNode().id;

        this.$activeDropNode.set(null);

        const files = event.dataTransfer?.files ?? undefined;

        if (files?.length) {
            this.uploadFiles.emit({ files, targetFolderId });
        }
    }
}
