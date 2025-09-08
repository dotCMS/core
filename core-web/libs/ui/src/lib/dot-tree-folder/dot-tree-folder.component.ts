import { ChangeDetectionStrategy, Component, input, output, computed } from '@angular/core';

import { TreeNode } from 'primeng/api';
import { TreeModule, TreeNodeExpandEvent, TreeNodeCollapseEvent } from 'primeng/tree';

import { DotMessagePipe } from '../dot-message/dot-message.pipe';
import { FolderNamePipe } from '../pipes/dot-folder-name/dot-folder-name.pipe';

export const SYSTEM_HOST_ID = 'SYSTEM_HOST';

@Component({
    selector: 'dot-tree-folder',
    imports: [TreeModule, FolderNamePipe, DotMessagePipe],
    templateUrl: './dot-tree-folder.component.html',
    styleUrls: ['./dot-tree-folder.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotTreeFolderComponent {
    /**
     * An observable that emits an array of TreeNode objects representing the folders.
     *
     * @type {Observable<TreeNode[]>}
     * @alias folders
     */
    $folders = input.required<TreeNode[]>({ alias: 'folders' });

    /**
     * A boolean observable that indicates the loading state.
     *
     * @type {boolean}
     */
    $loading = input.required<boolean>({ alias: 'loading' });

    /**
     * A signal that represents the selected node.
     *
     * @type {TreeNode | null}
     */
    $selectedNode = input.required<TreeNode>({ alias: 'selectedNode' });

    /**
     * Controls whether the folder icon should be shown only on the first root node's toggler.
     * When false, folder icons will be used for all togglers.
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
