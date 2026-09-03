import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';

import type { TreeNode } from 'primeng/api';
import type { TreeNodeExpandEvent, TreeNodeSelectEvent } from 'primeng/types/tree';

import { ComponentStatus, DotSite, TreeNodeItem } from '@dotcms/dotcms-models';

import { DotMessagePipe } from '../../../../dot-message/dot-message.pipe';
import { DotFolderNamePipe } from '../../../../pipes/dot-folder-name/dot-folder-name.pipe';
import { DotFolderSearchResultsComponent } from '../../../dot-folder-search-results/dot-folder-search-results.component';
import { DotFolderTreeComponent } from '../../../dot-folder-tree/dot-folder-tree.component';
import { DotSearchInputComponent } from '../../../dot-search-input/dot-search-input.component';
import { DotSiteComponent } from '../../../dot-site/dot-site.component';
import { DotTruncatedLabelComponent } from '../../../dot-truncated-label/dot-truncated-label.component';
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
    imports: [
        DotFolderTreeComponent,
        DotFolderSearchResultsComponent,
        DotSearchInputComponent,
        DotSiteComponent,
        DotFolderNamePipe,
        DotMessagePipe,
        DotTruncatedLabelComponent
    ],
    // Three rows now: site selector, folder search, then the tree taking what is left.
    host: { class: 'grid h-full w-full min-h-0 grid-rows-[min-content_min-content_1fr]' }
})
export class DotAssetPickerSidebarComponent {
    readonly store = inject(DotAssetPickerStore);

    protected readonly $loading = computed(
        () => this.store.foldersStatus() === ComponentStatus.LOADING
    );

    protected readonly $searching = computed(
        () => this.store.searchStatus() === ComponentStatus.LOADING
    );

    protected readonly $searchFailed = computed(
        () => this.store.searchStatus() === ComponentStatus.ERROR
    );

    protected readonly treePt = {
        // `p-0!` is required, not stylistic: `p-tree` ships its own padding from an unlayered
        // stylesheet, which beats Tailwind utilities in `@layer utilities`. Without the modifier
        // the tree keeps its own inset on top of the wrapper's `px-4` and its rows sit further
        // right than the two inputs above them.
        root: { class: 'w-full h-full min-w-0 overflow-x-hidden border-none p-0!' },
        wrapper: { class: 'min-w-0 overflow-x-hidden' },
        nodeContent: { class: 'min-w-0' },
        nodeLabel: { class: 'min-w-0 overflow-hidden' }
    };

    /**
     * Moves the picker to another site.
     *
     * A cleared selection is ignored rather than acted on: `DotSiteComponent` emits `null` when its
     * value is cleared, but the picker is always browsing *somewhere* — there is no "no site" state
     * to move to, and treating one as valid would leave the tree and the asset list unscoped.
     */
    protected onSiteChange(site: DotSite | null): void {
        if (!site) {
            return;
        }

        this.store.setBrowsingSite({
            identifier: site.identifier,
            hostname: site.hostname
        });
    }

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
