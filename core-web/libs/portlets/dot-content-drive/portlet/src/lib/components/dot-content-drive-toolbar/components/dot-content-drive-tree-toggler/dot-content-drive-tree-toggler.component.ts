import { ChangeDetectionStrategy, Component, HostListener, inject } from '@angular/core';

import { ButtonDirective } from 'primeng/button';

import { DotContentDriveStore } from '../../../../store/dot-content-drive.store';

@Component({
    selector: 'dot-content-drive-tree-toggler',
    templateUrl: './dot-content-drive-tree-toggler.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    hostDirectives: [ButtonDirective],
    host: {
        class: 'p-button-icon-only p-button-rounded p-button-text p-button'
    }
})
export class DotContentDriveTreeTogglerComponent {
    #store = inject(DotContentDriveStore);

    /**
     * Drives which of the two panel glyphs is showing, the way UVE swaps its palette icons.
     *
     * Reads the VISUAL state, not the stored preference: the Edit Content side panel can force the
     * tree collapsed on a narrow viewport without touching what the user chose, and the glyph has to
     * follow what is actually on screen. Clicking still toggles the preference itself.
     */
    readonly $treeExpanded = this.#store.isTreeVisuallyExpanded;

    @HostListener('click')
    toggleTree(): void {
        this.#store.setIsTreeExpanded(!this.#store.isTreeExpanded());
    }
}
