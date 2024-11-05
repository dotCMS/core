import { SlicePipe } from '@angular/common';
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    inject,
    input,
    output,
    signal,
    viewChild
} from '@angular/core';

import { TreeNode } from 'primeng/api';
import { SkeletonModule } from 'primeng/skeleton';
import { Tree, TreeModule, TreeNodeExpandEvent } from 'primeng/tree';

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
    $folders = input.required<TreeNode[]>({ alias: 'folders' });
    $loading = input.required<boolean>({ alias: 'loading' });

    $fakeColumns = signal<string[]>(
        Array.from({ length: 50 }).map((_) => `${this.getRandomRange(75, 100)}%`)
    );

    onNodeExpand = output<TreeNodeExpandEvent>();

    readonly #cd = inject(ChangeDetectorRef);
    $tree = viewChild.required(Tree);

    detectChanges() {
        this.#cd.detectChanges();
    }

    getRandomRange(max: number, min: number) {
        return Math.floor(Math.random() * (max - min + 1) + min);
    }
}
