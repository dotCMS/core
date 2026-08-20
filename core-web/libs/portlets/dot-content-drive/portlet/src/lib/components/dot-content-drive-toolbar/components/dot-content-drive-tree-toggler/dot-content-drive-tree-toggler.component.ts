import { ChangeDetectionStrategy, Component, inject } from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { DotMessagePipe } from '@dotcms/ui';

import { DotContentDriveStore } from '../../../../store/dot-content-drive.store';

@Component({
    selector: 'dot-content-drive-tree-toggler',
    templateUrl: './dot-content-drive-tree-toggler.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'flex' },
    imports: [ButtonModule, DotMessagePipe]
})
export class DotContentDriveTreeTogglerComponent {
    #store = inject(DotContentDriveStore);

    /**
     * Names the action in the button's accessible name. The glyph itself is static — it says where the
     * panel docks, not what the click does — so this is the only place the state is exposed.
     *
     * Reads the VISUAL state, not the stored preference: the Edit Content side panel can force the
     * tree collapsed on a narrow viewport without touching what the user chose, and the name has to
     * describe what the click will actually do from what is on screen.
     */
    readonly $treeExpanded = this.#store.isTreeVisuallyExpanded;

    /**
     * Whether the side panel is holding the tree collapsed, which disables this button.
     *
     * Toggling the stored preference while that is on moves nothing — the computed behind
     * {@link $treeExpanded} ands the two together — so the button would look live and do nothing.
     * Honoring the force-collapse means refusing the interaction until the panel releases it, rather
     * than clearing a collapse the panel owns.
     */
    readonly $treeForceCollapsed = this.#store.isTreeForceCollapsed;

    toggleTree(): void {
        this.#store.setIsTreeExpanded(!this.#store.isTreeExpanded());
    }
}
