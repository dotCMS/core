import { signalMethod } from '@ngrx/signals';

import { ChangeDetectionStrategy, Component, computed, input, output, signal } from '@angular/core';

import type { TreeNode } from 'primeng/api';
import { SkeletonModule } from 'primeng/skeleton';
import { Tooltip } from 'primeng/tooltip';
import type { TreeNodeExpandEvent, TreeNodeSelectEvent } from 'primeng/types/tree';

import { DotFolderNamePipe } from '../../../../pipes/dot-folder-name/dot-folder-name.pipe';
import { DotFolderTreeComponent } from '../../../dot-folder-tree/dot-folder-tree.component';
import { SYSTEM_HOST_ID } from '../../store/browser.store';

@Component({
    selector: 'dot-sidebar',
    imports: [DotFolderTreeComponent, DotFolderNamePipe, SkeletonModule, Tooltip],
    templateUrl: './dot-sidebar.component.html',
    styleUrls: ['./dot-sidebar.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotSideBarComponent {
    /**
     * Match Host Folder site tooltips: keep long hostnames on one line in the overlay.
     */
    protected readonly nodeTooltipPt = {
        root: { style: { maxWidth: 'none' } },
        text: { style: { whiteSpace: 'nowrap', wordBreak: 'normal' } }
    };

    /**
     * Constrain tree node layout so label `truncate` can ellipsis instead of wrapping.
     */
    protected readonly treePt = {
        root: { class: 'w-full h-full min-w-0 overflow-x-hidden' },
        wrapper: { class: 'min-w-0 overflow-x-hidden' },
        nodeContent: { class: 'min-w-0' },
        nodeLabel: { class: 'min-w-0 overflow-hidden' }
    };

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
     * @type {TreeNodeSelectEvent}
     */
    onNodeSelect = output<TreeNodeSelectEvent>();

    /**
     * Emitted when the synthetic "Load more" node is clicked.
     */
    loadMore = output<TreeNode>();

    readonly #userSelected = signal<TreeNode | null>(null);

    /**
     * Selected node for the shared tree. Defaults to SYSTEM_HOST when present and
     * the user has not selected another node yet.
     */
    readonly $selectedNode = computed(() => {
        return (
            this.#userSelected() ??
            this.$folders().find((folder) => folder.data?.id === SYSTEM_HOST_ID) ??
            null
        );
    });

    /**
     * When folders reload, clear a stale user selection that is no longer in the tree.
     * `signalMethod` only tracks its input (`$folders`), so `#userSelected` reads/writes
     * inside the processor stay untracked — no manual `untracked()` needed.
     * @see https://ngrx.io/guide/signals/signal-method
     */
    readonly #clearStaleSelection = signalMethod<TreeNode[]>((folders) => {
        const selected = this.#userSelected();

        if (!selected) {
            return;
        }

        if (!this.#nodeExists(folders, selected.key)) {
            this.#userSelected.set(null);
        }
    });

    constructor() {
        this.#clearStaleSelection(this.$folders);
    }

    /**
     * Forwards selection to the parent and tracks it for tree highlight.
     */
    handleNodeSelect(event: TreeNodeSelectEvent): void {
        this.#userSelected.set(event.node);
        this.onNodeSelect.emit(event);
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

    #nodeExists(nodes: TreeNode[], key: string | undefined): boolean {
        if (!key) {
            return false;
        }

        for (const node of nodes) {
            if (node.key === key) {
                return true;
            }

            if (node.children?.length && this.#nodeExists(node.children, key)) {
                return true;
            }
        }

        return false;
    }
}
