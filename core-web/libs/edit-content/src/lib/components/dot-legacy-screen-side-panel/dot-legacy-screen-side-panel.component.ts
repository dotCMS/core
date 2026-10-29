import { DOCUMENT } from '@angular/common';
import {
    Component,
    ElementRef,
    NgZone,
    OnDestroy,
    computed,
    inject,
    input,
    linkedSignal,
    output,
    viewChild
} from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

import { DotUiColorsService } from '@dotcms/data-access';
import { DotSidePanelContent, DotSidePanelRef } from '@dotcms/ui';
import { isSameOriginRelativeUrl } from '@dotcms/utils';

import { DotLegacyScreenEvent, DotLegacyScreenRequest } from '../../models/dot-legacy-screen.model';

/**
 * Any legacy (JSP) admin screen as a side panel's content (#38303): the content selector, the menu
 * reorder screen, the translate screen, and the legacy content editor, which is built on top of
 * this one (`DotLegacyEditorSidePanelComponent`).
 *
 * It only loads the screen and reports what the screen says. It applies the admin UI colors, covers
 * the iframe while a document loads, forwards an Escape pressed inside the screen to the admin page
 * so the panel's own Escape rule decides, and passes every `ng-event` the screen dispatches through
 * {@link legacyEvent}, unchanged. What an event means is up to whoever opened the screen.
 */
@Component({
    selector: 'dot-legacy-screen-side-panel',
    // Fills the drawer, so the iframe can take its whole height.
    host: { class: 'flex flex-1' },
    templateUrl: './dot-legacy-screen-side-panel.component.html'
})
export class DotLegacyScreenSidePanelComponent implements DotSidePanelContent, OnDestroy {
    readonly #panel = inject(DotSidePanelRef);
    readonly #document = inject(DOCUMENT);
    readonly #uiColors = inject(DotUiColorsService);
    readonly #sanitizer = inject(DomSanitizer);
    readonly #zone = inject(NgZone);

    /** The screen to load, or `null` to load nothing. */
    readonly request = input<DotLegacyScreenRequest | null>(null);

    /** Every `ng-event` the screen dispatches on its own document, unchanged. */
    readonly legacyEvent = output<DotLegacyScreenEvent>();

    /** The screen's document loaded (the first load or a reload), with the screen's window. */
    readonly loaded = output<Window>();

    /** The iframe the screen loads in. */
    protected readonly $iframe = viewChild<ElementRef<HTMLIFrameElement>>('iframe');

    /**
     * The URL to load, as a string, or `null` when it is not on this server. A string, so a request
     * that changes only its title never rebinds the iframe's `src`, which would reload the screen.
     */
    readonly #src = computed(() => {
        const url = this.request()?.url;

        return url && isSameOriginRelativeUrl(url) ? url : null;
    });

    /** The URL the iframe loads, or `null` when there is nothing valid to load. */
    protected readonly $url = computed<SafeResourceUrl | null>(() => {
        const src = this.#src();

        return src ? this.#sanitizer.bypassSecurityTrustResourceUrl(src) : null;
    });

    /**
     * Whether the screen is loading a document: set again whenever the panel loads a new URL, and
     * while the screen reloads itself. The iframe shows nothing useful until then, so a spinner
     * covers it.
     */
    protected readonly $loading = linkedSignal(() => this.#src() !== null);

    /** Removes the listeners attached to the current iframe document; replaced on every load. */
    #detachFrame: () => void = () => undefined;

    constructor() {
        // No padding, so the screen's own layout reaches the drawer's edges; a flex row, so the
        // iframe takes the drawer's whole height.
        this.#panel.contentStyleClass.set('p-0! flex');
    }

    /**
     * Close intent (the X, Escape, a mask click): closes at once. A guard, if any, belongs to the
     * content built on top.
     */
    requestClose(): void {
        this.#panel.close();
    }

    /**
     * Runs every time the iframe loads a document. A legacy screen replaces its whole document on a
     * reload, so the listeners are attached again each time, and the old ones dropped.
     */
    protected onIframeLoad(): void {
        this.#detachFrame();
        this.#detachFrame = () => undefined;
        this.$loading.set(false);

        const screenWindow = this.$iframe()?.nativeElement.contentWindow;
        const doc = screenWindow?.document;

        if (!screenWindow || !doc) {
            return;
        }

        const html = doc.querySelector('html');
        if (html) {
            this.#uiColors.setColors(html);
        }

        const onLegacyEvent = (event: Event) =>
            this.#zone.run(() =>
                this.legacyEvent.emit((event as CustomEvent<DotLegacyScreenEvent>).detail)
            );

        // A keydown inside the iframe never reaches the admin page, so the shortcut registry cannot
        // see it: press Escape again on the admin page, where the side panel's own Escape rules
        // decide. Not one the screen already handled: a dialog inside the screen that closes on
        // Escape is only told apart by having consumed the key.
        const onKeydown = (event: KeyboardEvent) => {
            if (event.key === 'Escape' && !event.defaultPrevented) {
                this.#zone.run(() =>
                    this.#document.dispatchEvent(
                        new KeyboardEvent('keydown', {
                            key: 'Escape',
                            bubbles: true,
                            cancelable: true
                        })
                    )
                );
            }
        };

        // The screen is leaving this document for a new one: cover it until the next `load`.
        const onPageHide = () => this.#zone.run(() => this.$loading.set(true));

        doc.addEventListener('ng-event', onLegacyEvent);
        doc.addEventListener('keydown', onKeydown);
        screenWindow.addEventListener('pagehide', onPageHide);

        this.#detachFrame = () => {
            doc.removeEventListener('ng-event', onLegacyEvent);
            doc.removeEventListener('keydown', onKeydown);
            screenWindow.removeEventListener('pagehide', onPageHide);
        };

        this.loaded.emit(screenWindow);
    }

    /** Drops the iframe listeners. The side panel releases its own registrations. */
    ngOnDestroy(): void {
        this.#detachFrame();
    }
}
