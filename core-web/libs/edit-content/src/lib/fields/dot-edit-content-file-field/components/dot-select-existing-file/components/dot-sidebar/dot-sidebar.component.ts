import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { TreeNode } from 'primeng/api';
import { TreeModule } from 'primeng/tree';

@Component({
    selector: 'dot-sidebar',
    standalone: true,
    imports: [TreeModule],
    templateUrl: './dot-sidebar.component.html',
    styleUrls: ['./dot-sidebar.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotSideBarComponent {
    /**
     * An observable that emits an array of TreeNode objects representing folders.
     *
     * @type {Observable<TreeNode[]>}
     * @alias folders
     */
    $folders = input.required<TreeNode[]>({ alias: 'folders' });
    /**
     * Represents a loading state for the component.
     *
     * @type {boolean}
     * @alias loading
     */
    $loading = input.required<boolean>({ alias: 'loading' });
}
