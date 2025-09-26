import { ChangeDetectionStrategy, Component, input, output, computed } from '@angular/core';

import { TreeNode } from 'primeng/api';
import { TreeModule, TreeNodeExpandEvent, TreeNodeCollapseEvent } from 'primeng/tree';

import { DotMessagePipe, FolderNamePipe } from '@dotcms/ui';

@Component({
    selector: 'dot-tree-folder',
    imports: [TreeModule, FolderNamePipe, DotMessagePipe],
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
    $showFolderIconOnFirstOnly = input<boolean>(false, { alias: 'showFolderIconOnFirstOnly' });

    /**
     * Computed style classes for the underlying p-tree component.
     */
    treeStyleClasses = computed(
        () => `w-full h-full ${this.$showFolderIconOnFirstOnly() ? 'first-only' : 'folder-all'}`
    );

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
}
