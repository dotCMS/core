import { Component, DestroyRef, inject, input, output, signal, viewChild } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { Drawer, DrawerModule } from 'primeng/drawer';

import { DotTitleStatus } from '@dotcms/dotcms-models';

import { DotMessagePipe } from '../../dot-message/dot-message.pipe';
import { DotKeyboardShortcutService } from '../../services/dot-keyboard-shortcut/dot-keyboard-shortcut.service';
import { hasOverlayAbove } from '../../services/dot-keyboard-shortcut/overlay';
import { DotContentletStatusBadgeComponent } from '../dot-contentlet-status-badge/dot-contentlet-status-badge.component';

/**
 * Shared by every side panel, so the author's expanded (full-width) choice carries over between
 * them.
 */
const EXPANDED_STORAGE_KEY = 'dot-edit-content-side-panel-expanded';

/** Best-effort read of the expanded preference; `false` when storage is unavailable. */
function readExpandedPreference(): boolean {
    try {
        return localStorage.getItem(EXPANDED_STORAGE_KEY) === 'true';
    } catch {
        return false;
    }
}

/** Persists the expanded preference; storage failures must not break the panel. */
function writeExpandedPreference(expanded: boolean): void {
    try {
        localStorage.setItem(EXPANDED_STORAGE_KEY, String(expanded));
    } catch {
        // best-effort: quota errors / disabled storage are ignored.
    }
}

/**
 * The drawer of one open side panel. `DotSidePanelOutletComponent` renders one for each panel
 * `DotSidePanelService` has open, and puts the panel's content inside; nothing else mounts it.
 *
 * It is open for as long as it is mounted: from the moment it exists it claims Escape and listens
 * for clicks on its mask. What the panels share lives here:
 *
 * - **The drawer and its header**: a transparent mask, 80% wide or the full width, the title, and
 *   the expand and close buttons.
 * - **Escape** asks to close, through the shortcut registry rather than a listener of its own: two
 *   document listeners cannot arbitrate (`preventDefault` does not stop the other one), so the
 *   portlet behind would act on the same key. Escape is always consumed while the panel is open, and
 *   only asks to close when no overlay sits above the panel and it is the frontmost one.
 * - **A click on its own mask** asks to close too, when it is the frontmost panel. The mask is
 *   matched by identity, not by its `p-drawer-mask` class, which every modal drawer shares (the UVE
 *   block editor sidebar is one); a click inside the panel, or a drag that starts inside and ends on
 *   the mask, resolves to another node and is ignored.
 * - **Full width**: the expanded preference, shared by every side panel.
 *
 * The page editor's experiments panel (`dot-experiments-panel`) keeps its own copy of these rules,
 * with a different width and header. Its class docs say when it moves here; until then, a fix here
 * belongs there too.
 */
@Component({
    selector: 'dot-side-panel',
    imports: [DrawerModule, ButtonModule, DotMessagePipe, DotContentletStatusBadgeComponent],
    templateUrl: './dot-side-panel.component.html',
    // On the document because `appendTo="body"` moves the drawer and its mask out of this
    // component's DOM, so no template binding can hear the mask click.
    host: {
        '(document:click)': 'onDocumentClick($event)'
    }
})
export class DotSidePanelComponent {
    /**
     * Whether this is the frontmost open panel, the only one Escape and a click on the mask close.
     * The outlet sets it: the last panel opened is in front.
     */
    readonly front = input(true);

    /**
     * The title in the panel's header. Not `header`: inside the template, the drawer's `#header`
     * slot would shadow it.
     */
    readonly heading = input('');

    /**
     * The chip shown right after the heading, or `null` for none. The heading truncates; the chip
     * keeps its size.
     */
    readonly titleStatus = input<DotTitleStatus>(null);

    /** Classes for the content area. No padding by default, for content that lays itself out. */
    readonly contentStyleClass = input('p-0!');

    /** The close button, Escape or a click on the mask: the panel's content decides what follows. */
    readonly closeRequested = output<void>();

    /** The drawer: its mask and container are what Escape and the mask click check. */
    protected readonly $drawer = viewChild(Drawer);

    /** Whether the panel spans the full width (vs 80%), seeded from the stored choice. */
    protected readonly $expanded = signal(readExpandedPreference());

    constructor() {
        const withdrawEscape = inject(DotKeyboardShortcutService).register({
            combination: 'escape',
            label: 'edit.content.side-panel.shortcut.close',
            handler: () => this.#escape()
        });

        inject(DestroyRef).onDestroy(withdrawEscape);
    }

    /**
     * Any click on the page: asks to close when it lands on this panel's own mask and this is the
     * frontmost panel.
     *
     * @param event the click
     */
    protected onDocumentClick(event: MouseEvent): void {
        if (event.target === this.$drawer()?.mask && this.front()) {
            this.closeRequested.emit();
        }
    }

    /** Flips the full-width state and remembers it for the next panel. */
    protected toggleExpanded(): void {
        const expanded = !this.$expanded();

        this.$expanded.set(expanded);
        writeExpandedPreference(expanded);
    }

    /**
     * Escape, as the panel handles it.
     *
     * @returns `true`: the key is consumed.
     */
    #escape(): boolean {
        // An overlay above owns the key. Consumed, not declined, so it cannot reach the portlet.
        if (hasOverlayAbove(this.$drawer()?.container)) {
            return true;
        }

        if (this.front()) {
            this.closeRequested.emit();
        }

        return true;
    }
}
