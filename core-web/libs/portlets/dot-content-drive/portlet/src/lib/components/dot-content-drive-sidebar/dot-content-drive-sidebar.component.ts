import { ChangeDetectionStrategy, Component, effect, inject, untracked } from '@angular/core';

import { TreeNodeCollapseEvent, TreeNodeExpandEvent, TreeNodeSelectEvent } from 'primeng/tree';

import { DotTreeFolderComponent } from '@dotcms/ui';

import { DotContentDriveStore } from '../../store/dot-content-drive.store';
import { DotContentDriveTreeTogglerComponent } from '../dot-content-drive-toolbar/components/dot-content-drive-tree-toggler/dot-content-drive-tree-toggler.component';

@Component({
    selector: 'dot-content-drive-sidebar',
    templateUrl: './dot-content-drive-sidebar.component.html',
    styleUrl: './dot-content-drive-sidebar.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [DotTreeFolderComponent, DotContentDriveTreeTogglerComponent]
})
export class DotContentDriveSidebarComponent {
    readonly #store = inject(DotContentDriveStore);

    readonly $loading = this.#store.sidebarLoading;
    readonly $folders = this.#store.folders;
    readonly $selectedNode = this.#store.selectedNode;
    readonly $currentSite = this.#store.currentSite;

    readonly getSiteFoldersEffect = effect(() => {
        const currentSite = this.$currentSite();
        if (!currentSite) {
            return;
        }

        // Use untracked to prevent path changes from triggering this effect
        // Only reload folders when the site changes, not when user selects nodes
        untracked(() => {
            this.#store.loadFolders();
        });
    });

    /**
     * Handles node selection events - Only changes the selected folder content, not tree expansion
     *
     * @param {TreeNodeSelectEvent} event - The tree node select event
     */
    protected onNodeSelect(event: TreeNodeSelectEvent): void {
        const { node } = event;
        const { path } = node.data;

        // Just change the path to load the folder content, don't trigger tree reloading
        // The tree expansion should only happen through onNodeExpand or URL navigation
        this.#store.setPath(path, 'selection');
    }

    /**
     * Handles node expansion events and loads child folders
     *
     * @param {TreeNodeExpandEvent} event - The tree node expand event
     */
    protected onNodeExpand(event: TreeNodeExpandEvent): void {
        const { node } = event;
        const { hostname, path } = node.data;
        const fullPath = `${hostname}${path}`;

        // Update expanded state in store
        if (node.key) {
            this.#store.updateExpandedState(node.key, true);
        }

        // If node already has children or is a leaf, just expand it
        if (node.children?.length > 0 || node.leaf) {
            node.expanded = true;
            return;
        }

        // Load children if not already loaded
        node.loading = true;
        this.#store.loadChildFolders(fullPath).subscribe(({ folders }) => {
            node.loading = false;
            node.expanded = true;
            node.leaf = folders.length === 0;
            node.children = [...folders];

            // Update folders without triggering any effects
            this.#store.updateFolders([...this.$folders()]);
        });
    }

    /**
     * Handles node collapse events
     * Prevents collapse of the special 'ALL_FOLDER' node
     *
     * @param {TreeNodeCollapseEvent} event - The tree node collapse event
     */
    protected onNodeCollapse(event: TreeNodeCollapseEvent): void {
        const { node } = event;

        if (node.key === 'ALL_FOLDER') {
            node.expanded = true;
            return;
        }

        // Update expanded state in store
        if (node.key) {
            this.#store.updateExpandedState(node.key, false);
        }
    }
}
