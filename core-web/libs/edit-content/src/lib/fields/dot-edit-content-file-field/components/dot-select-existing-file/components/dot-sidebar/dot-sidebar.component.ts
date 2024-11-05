import { faker } from '@faker-js/faker';

import { SlicePipe } from '@angular/common';
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    inject,
    input,
    output,
    signal
} from '@angular/core';

import { TreeNode } from 'primeng/api';
import { SkeletonModule } from 'primeng/skeleton';
import { TreeModule, TreeNodeExpandEvent } from 'primeng/tree';

import { TruncatePathPipe } from '../../../../../../pipes/truncate-path.pipe';

@Component({
    selector: 'dot-sidebar',
    standalone: true,
    imports: [TreeModule, SlicePipe, TruncatePathPipe, SkeletonModule],
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
    $fakeColumns = signal<string[]>(
        Array.from({ length: 50 }).map((_) => `${this.getRandomRange(75, 100)}%`)
    );

    /**
     * Event emitter for when a tree node is expanded.
     *
     * This event is triggered when a user expands a node in the tree structure.
     * It emits an event of type `TreeNodeExpandEvent`.
     */
    onNodeExpand = output<TreeNodeExpandEvent>();

    /**
     * Triggers change detection manually.
     * This method is used to ensure that the view is updated when the model changes.
     * It calls the `detectChanges` method on the ChangeDetectorRef instance.
     */
    detectChanges() {
        this.#cd.detectChanges();
    }

    /**
     * Generates a random integer within a specified range.
     *
     * @param max - The maximum value of the range (inclusive).
     * @param min - The minimum value of the range (inclusive).
     * @returns A random integer between min and max (both inclusive).
     */
    getRandomRange(max: number, min: number) {
        return faker.number.int({ max, min });
    }
}
