import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';

import type { TreeNode } from 'primeng/api';
import type { TreeNodeExpandEvent, TreeNodeSelectEvent } from 'primeng/types/tree';

import { ComponentStatus, TreeNodeItem } from '@dotcms/dotcms-models';

import { DotFolderNamePipe } from '../../../../pipes/dot-folder-name/dot-folder-name.pipe';
import { DotFolderTreeComponent } from '../../../dot-folder-tree/dot-folder-tree.component';
import { DotSearchInputComponent } from '../../../dot-search-input/dot-search-input.component';
import { DotAssetPickerStore } from '../../store/dot-asset-picker.store';

/**
 * Sites-and-folders navigation for the AssetPicker: a search box over a tree whose roots are the
 * sites the user can browse.
 *
 * Selecting a node scopes the asset list to it — a site root to that whole site, a folder to that
 * folder. Unlike Content Drive's sidebar this one has no drag-and-drop of rows and no tree toggler
 * (a dialog has nothing to collapse into), so it binds the presentational
 * {@link DotFolderTreeComponent} directly and leaves all loading to the store.
 */
@Component({
    selector: 'dot-asset-picker-sidebar',
    templateUrl: './dot-asset-picker-sidebar.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [DotFolderTreeComponent, DotSearchInputComponent, DotFolderNamePipe],
    host: { class: 'grid h-full w-full min-h-0 grid-rows-[min-content_1fr]' }
})
export class DotAssetPickerSidebarComponent {
    readonly store = inject(DotAssetPickerStore);

    protected readonly $loading = computed(
        () => this.store.foldersStatus() === ComponentStatus.LOADING
    );

    protected readonly treePt = {
        root: { class: 'w-full h-full min-w-0 overflow-x-hidden border-none' },
        wrapper: { class: 'min-w-0 overflow-x-hidden' },
        nodeContent: { class: 'min-w-0' },
        nodeLabel: { class: 'min-w-0 overflow-hidden' }
    };

    /**
     * Scopes the list to the selected node. Content Drive needs an effect for this to resolve a
     * race with its URL restore; the picker is configured explicitly, so a direct call is enough.
     */
    protected onNodeSelect(event: TreeNodeSelectEvent): void {
        this.store.selectNode(event.node as TreeNodeItem);
    }

    /** Lazily loads a node's first page of children the first time it is expanded. */
    protected onNodeExpand(event: TreeNodeExpandEvent): void {
        this.store.expandNode(event.node as TreeNodeItem);
    }

    /** Loads the next page of a level when its "Load more" sentinel is clicked. */
    protected onLoadMore(node: TreeNode): void {
        this.store.loadMore(node as TreeNodeItem);
    }
}
