import { DOCUMENT, NgTemplateOutlet } from '@angular/common';
import {
    Component,
    DestroyRef,
    TemplateRef,
    afterNextRender,
    contentChild,
    inject,
    input,
    output,
    signal,
    viewChild
} from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { Drawer, DrawerModule } from 'primeng/drawer';

import { DotKeyboardShortcutService, DotMessagePipe, hasOverlayAbove } from '@dotcms/ui';

import { DotSidePanelNavController } from '../../services/dot-side-panel-nav.service';

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
 * The drawer every side panel opens in: Edit Content, the legacy editor and Edit Source. A panel
 * puts its content inside, and an optional `<ng-template #footer>`, and handles
 * {@link closeRequested} with its own unsaved-changes guard.
 *
 * **Mount it only while the panel is open**, behind an `@if` or a `ComponentRef` the owner
 * destroys, and close it by unmounting it. It has no hidden state: from the moment it exists it
 * claims Escape, listens for clicks on its mask and joins the side-panel stack, which collapses the
 * main navigation, so a mounted but hidden panel would swallow Escape across the whole portlet.
 *
 * What the panels share lives here:
 *
 * - **The drawer and its header**: a transparent mask, 80% wide or the full width, the title, and
 *   the expand and close buttons.
 * - **Escape** asks to close, through the shortcut registry rather than a listener of its own: two
 *   document listeners cannot arbitrate (`preventDefault` does not stop the other one), so the
 *   portlet behind would act on the same key. Escape is always consumed while the panel is open, and
 *   only asks to close when no overlay sits above the panel and it is the frontmost one.
 * - **A click on its own mask** asks to close too. The mask is matched by identity, not by its
 *   `p-drawer-mask` class, which every modal drawer shares (the UVE block editor sidebar is one); a
 *   click inside the panel, or a drag that starts inside and ends on the mask, resolves to another
 *   node and is ignored.
 * - **The side-panel stack**: joined after the first render (which collapses the main navigation)
 *   and left on destroy.
 * - **Full width**: the expanded preference, shared by every side panel.
 *
 * The page editor's experiments panel (`dot-experiments-panel`) keeps its own copy of these rules,
 * with a different width and header. Its class docs say when it moves here; until then, a fix here
 * belongs there too.
 */
@Component({
    selector: 'dot-side-panel',
    imports: [DrawerModule, ButtonModule, NgTemplateOutlet, DotMessagePipe],
    templateUrl: './dot-side-panel.component.html'
})
export class DotSidePanelComponent {
    readonly #navController = inject(DotSidePanelNavController);

    /**
     * The title in the panel's header. Not `header`: inside the template, the drawer's `#header`
     * slot would shadow it.
     */
    readonly heading = input('');

    /** Classes for the content area. No padding by default, for content that lays itself out. */
    readonly contentStyleClass = input('p-0!');

    /**
     * The close button, Escape or a click on the mask. The owner runs its guard, then closes the
     * panel by unmounting it.
     */
    readonly closeRequested = output<void>();

    /** The owner's footer. The drawer draws its footer bar only when there is one. */
    protected readonly $footer = contentChild<TemplateRef<unknown>>('footer');

    /** The drawer: its mask and container are what Escape and the mask click check. */
    protected readonly $drawer = viewChild(Drawer);

    /** Whether the panel spans the full width (vs 80%), seeded from the stored choice. */
    protected readonly $expanded = signal(readExpandedPreference());

    constructor() {
        const withdrawEscape = inject(DotKeyboardShortcutService).register({
            combination: 'escape',
            label: 'edit.content.side-panel.shortcut.close',
            handler: () => this.escape()
        });

        // Document-wide because `appendTo="body"` moves the drawer and its mask out of this
        // component's DOM, so no template binding can hear the mask click.
        const document = inject(DOCUMENT);
        const onDocumentClick = (event: MouseEvent): void => {
            if (event.target === this.$drawer()?.mask && this.#navController.isTop(this)) {
                this.closeRequested.emit();
            }
        };
        document.addEventListener('click', onDocumentClick);

        // After the current render, so the store mutation it causes does not land during it.
        afterNextRender(() => this.#navController.acquire(this));

        inject(DestroyRef).onDestroy(() => {
            this.#navController.release(this);
            withdrawEscape();
            document.removeEventListener('click', onDocumentClick);
        });
    }

    /**
     * Escape, as the panel handles it. Public for keys the shortcut registry cannot see, such as
     * Escape pressed inside an iframe the panel hosts.
     *
     * @returns `true`: the key is consumed.
     */
    escape(): boolean {
        // An overlay above owns the key. Consumed, not declined, so it cannot reach the portlet.
        if (hasOverlayAbove(this.$drawer()?.container)) {
            return true;
        }

        if (this.#navController.isTop(this)) {
            this.closeRequested.emit();
        }

        return true;
    }

    /** Flips the full-width state and remembers it for the next panel. */
    protected toggleExpanded(): void {
        const expanded = !this.$expanded();

        this.$expanded.set(expanded);
        writeExpandedPreference(expanded);
    }
}
