import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    computed,
    inject,
    input,
    model,
    output,
    signal
} from '@angular/core';

import { TreeNode } from 'primeng/api';
import { SkeletonModule } from 'primeng/skeleton';
import { TreeModule, TreeNodeExpandEvent } from 'primeng/tree';

import { DotTruncatePathPipe } from '../../../../pipes/dot-truncate-path/dot-truncate-path.pipe';
import { SYSTEM_HOST_ID } from '../../store/browser.store';

@Component({
    selector: 'dot-sidebar',
    imports: [TreeModule, DotTruncatePathPipe, SkeletonModule],
    templateUrl: './dot-sidebar.component.html',
    styleUrls: ['./dot-sidebar.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotSideBarComponent {
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
     * Signal that generates an array of strings representing percentages.
     * Each percentage is a random value between 75% and 100%.
     * The array contains 50 elements.
     *
     * @returns {string[]} An array of 50 percentage strings.
     */
    $fakeColumns = signal<string[]>(Array.from({ length: 50 }).map((_) => this.getPercentage()));

    /**
     * Reactive model representing the currently selected file.
     */
    $selectedFile = model<TreeNode | null>(null);

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
     * Computed property representing the component's state.
     *
     * @returns An object containing:
     * - `folders`: An array of folders obtained from `$folders()`.
     * - `selectedFile`: A signal of the selected file, initialized to the file whose `data.identifier` matches `SYSTEM_HOST_ID`.
     */
    $state = computed(() => {
        const folders = this.$folders();

        const selectedFile = folders.find((f) => f.data.identifier === SYSTEM_HOST_ID);

        return {
            folders,
            selectedFile: signal(selectedFile)
        };
    });

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
