import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    inject,
    input,
    output,
    signal,
    computed
} from '@angular/core';

import { TreeNode } from 'primeng/api';
import { SkeletonModule } from 'primeng/skeleton';
import { TreeModule, TreeNodeExpandEvent, TreeNodeCollapseEvent } from 'primeng/tree';

import { FolderNamePipe } from '../pipes/dot-folder-name/dot-folder-name.pipe';

export const SYSTEM_HOST_ID = 'SYSTEM_HOST';

@Component({
    selector: 'dot-tree-folder',
    imports: [TreeModule, SkeletonModule, FolderNamePipe],
    templateUrl: './dot-tree-folder.component.html',
    styleUrls: ['./dot-tree-folder.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotTreeFolderComponent {
    /**
     * A readonly private field that holds an instance of ChangeDetectorRef.
     * This is used to detect and respond to changes in the component's data-bound properties.
     */
    readonly #cd = inject(ChangeDetectorRef);
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
     * Signal that generates an array of strings representing percentages.
     * Each percentage is a random value between 75% and 100%.
     * The array contains 50 elements.
     *
     * @returns {string[]} An array of 50 percentage strings.
     */
    $fakeColumns = signal<string[]>(Array.from({ length: 50 }).map((_) => this.getPercentage()));

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
     * Triggers change detection manually.
     * This method is used to ensure that the view is updated when the model changes.
     * It calls the `detectChanges` method on the ChangeDetectorRef instance.
     */
    detectChanges() {
        this.#cd.detectChanges();
    }
    /**
     * Generates a random percentage string between 75% and 100%.
     *
     * @returns {string} A string representing a percentage between 75% and 100%.
     */
    getPercentage(): string {
        const number = Math.floor(Math.random() * (100 - 75 + 1)) + 75;

        return `${number}%`;
    }
}
