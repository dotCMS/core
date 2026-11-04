import { Subscription } from 'rxjs';

import { Location, PopStateEvent } from '@angular/common';
import { Injectable, inject } from '@angular/core';
import {
    Event as RouterEvent,
    NavigationCancel,
    NavigationCancellationCode,
    NavigationEnd,
    NavigationError,
    NavigationSkipped,
    Params,
    Router,
    UrlSerializer,
    UrlTree
} from '@angular/router';

import { filter, take } from 'rxjs/operators';

import { DotSidePanelRef } from '@dotcms/ui';
import { DOT_PANEL_URL_PARAM } from '@dotcms/utils';

/** An open panel written to the URL, and the params that name it. */
interface DotPanelUrlEntry {
    readonly ref: DotSidePanelRef;
    params: Record<string, string>;
    /** The surface's path, without its query string, as it was when the panel opened. */
    readonly path: string;
}

/** How {@link DotPanelUrlStateService.register} writes a panel. */
export interface DotPanelUrlRegisterOptions {
    /**
     * Rewrites the current history entry instead of adding one: for a panel that takes the place of
     * one the surface has just closed, such as Content Drive's reopen in the legacy editor. Remove
     * with the legacy editor.
     */
    inPlace?: boolean;
}

/**
 * Whether a router event ends the navigation that answers a Back, however it ended. Not a cancel
 * because a newer navigation superseded it (a second Back pressed before the first one's ended):
 * that newer navigation is still running, and its own `replaceState` would undo a restore written
 * now, so the wait goes on until it ends.
 */
const endsNavigation = (event: RouterEvent): boolean =>
    event instanceof NavigationEnd ||
    (event instanceof NavigationCancel &&
        event.code !== NavigationCancellationCode.SupersededByNewNavigation) ||
    event instanceof NavigationError ||
    event instanceof NavigationSkipped;

/** The params that name a panel on any surface; none of them is ever a surface's own. */
const PANEL_PARAMS: readonly string[] = Object.values(DOT_PANEL_URL_PARAM);

/**
 * Writes the open side panels to the surface's URL (#38303, FR-044 to FR-048). The opener
 * registers every panel opened with its `url` option here; a surface never writes the URL itself.
 *
 * Keeps a stack of the open panels, in open order, and the URL names the front one, its params
 * merged with the surface's own. Opening a panel adds one history entry; a language switch, a
 * create's first save and a close rewrite the URL in place, so Forward never reopens a closed
 * panel. When Back or Forward takes the front panel's params away, they are put back while the
 * panel's unsaved-changes guard decides, and only that panel is asked to close.
 *
 * The router answers every Back with a navigation of its own once it has navigated (dotcms-ui
 * sets `onSameUrlNavigation: 'reload'`), and that navigation ends with a `replaceState` of the URL
 * Back landed on. So the params are put back once that navigation is over, or a restore would be
 * overwritten while the prompt is open (FR-047). A Back that lands on another path has left the
 * surface: nothing is put back, nothing is asked to close, and the panel goes with the surface.
 *
 * The URL is read from and written to `Location` only. The surface's own params are read from
 * `Location` rather than the router, which does not see URLs written with `replaceState` (the
 * Query Tool's search params, say). And it is never written through the router: the legacy iframe
 * reloads on every query param change the router reports, and a router navigation recreates a
 * `reuseRoute: false` portlet (FR-046).
 */
@Injectable({ providedIn: 'root' })
export class DotPanelUrlStateService {
    readonly #location = inject(Location);
    readonly #serializer = inject(UrlSerializer);
    readonly #router = inject(Router);

    /** The open panels written to the URL, in open order: the last one is in front. */
    readonly #entries: DotPanelUrlEntry[] = [];

    /** Whether Back and Forward are being listened to; from the first panel written on. */
    #listening = false;

    /** The wait for the router's navigation that answers a Back, while one is pending. */
    #settling: Subscription | null = null;

    /**
     * Names an open panel in the URL, with one history entry, and forgets it when it closes. Writes
     * nothing when the URL already names the panel, as it does for a link opened on load: a second
     * entry for the same URL would make Back do nothing.
     *
     * @param ref The panel.
     * @param params The params that name it: `editContent` and `editContentLang`, `createContent`
     * or `editCode`, and any extra params the surface asked for.
     * @param options `inPlace` rewrites the current history entry instead of adding one.
     */
    register(
        ref: DotSidePanelRef,
        params: Record<string, string>,
        options: DotPanelUrlRegisterOptions = {}
    ): void {
        if (ref.isClosed) {
            return;
        }

        this.#listen();

        const entry: DotPanelUrlEntry = {
            ref,
            params: { ...params },
            path: this.#pathOf(this.#location.path(true))
        };
        this.#entries.push(entry);
        ref.closed.subscribe(() => this.#remove(entry));

        this.#write(options.inPlace ? 'replace' : 'push');
    }

    /**
     * Renames a registered panel in place, without a history entry: a language switch, or a create
     * that became an edit on its first save. A panel behind the front one is renamed for when it
     * is in front again.
     *
     * @param ref The panel.
     * @param params The params that name it now; they replace the ones it had.
     */
    update(ref: DotSidePanelRef, params: Record<string, string>): void {
        const entry = this.#entries.find((candidate) => candidate.ref === ref);

        if (!entry) {
            return;
        }

        const previous = Object.keys(entry.params);
        entry.params = { ...params };

        if (entry === this.#front()) {
            this.#write('replace', previous);
        }
    }

    /**
     * Removes every panel param, and `extraKeys`, from the URL in place. For a surface whose URL
     * names more than one panel on load: it opens none of them (FR-049).
     *
     * @param extraKeys Params of the surface's own that go with the panel params.
     */
    clear(extraKeys: readonly string[] = []): void {
        this.#write('replace', extraKeys);
    }

    /**
     * Listens to Back and Forward, once. A root service lives as long as the app, so the
     * subscription needs no teardown.
     */
    #listen(): void {
        if (this.#listening) {
            return;
        }

        this.#listening = true;
        this.#location.subscribe((event) => this.#onHistoryMove(event));
    }

    /**
     * Back or Forward moved the URL. On the surface's own path, the front panel is checked once the
     * router has answered the move: at once when the router has never navigated, since then no
     * navigation answers it.
     *
     * Under hash routing `Location` reports a `hashchange` after the `popstate` of the same move;
     * the router answers only the `popstate`, and so does this.
     */
    #onHistoryMove({ url, type }: PopStateEvent): void {
        const front = this.#front();

        if (type === 'hashchange' || this.#settling || !front) {
            return;
        }

        if (this.#pathOf(url ?? '') !== front.path) {
            return;
        }

        if (!this.#router.navigated) {
            this.#restoreFront();

            return;
        }

        this.#settling = this.#router.events.pipe(filter(endsNavigation), take(1)).subscribe(() => {
            this.#settling = null;
            this.#restoreFront();
        });
    }

    /** The front panel's entry, if any panel is open. */
    #front(): DotPanelUrlEntry | undefined {
        return this.#entries[this.#entries.length - 1];
    }

    /**
     * Forgets a closed panel. When it was in front, the URL names the panel underneath again, or
     * no panel, in place. Not when Back has left the surface: the URL is another screen's then.
     */
    #remove(entry: DotPanelUrlEntry): void {
        const index = this.#entries.indexOf(entry);

        if (index === -1) {
            return;
        }

        const wasFront = index === this.#entries.length - 1;
        this.#entries.splice(index, 1);

        if (wasFront && this.#pathOf(this.#location.path(true)) === entry.path) {
            this.#write('replace', Object.keys(entry.params));
        }
    }

    /**
     * When the URL no longer names the front panel, names it again, in place, and asks that panel
     * to close through its guard: discarding closes it, which clears the params; keeping on editing
     * leaves the panel and the URL as they are (FR-047).
     */
    #restoreFront(): void {
        const front = this.#front();

        if (!front) {
            return;
        }

        const url = this.#location.path(true);

        if (this.#pathOf(url) !== front.path) {
            return;
        }

        const { queryParams } = this.#serializer.parse(url);
        const namesFront = Object.entries(front.params).every(
            ([key, value]) => queryParams[key] === value
        );

        if (namesFront) {
            return;
        }

        this.#write('replace');
        front.ref.requestClose();
    }

    /**
     * A URL's path, without its query string or fragment.
     *
     * @param url A URL as `Location` reports it.
     */
    #pathOf(url: string): string {
        return this.#serializer.serialize(new UrlTree(this.#serializer.parse(url).root));
    }

    /**
     * Writes the current URL with every panel param taken out and the front panel's put in, the
     * surface's own params left as they are. Nothing is written when that changes nothing.
     *
     * @param mode `push` adds a history entry, `replace` rewrites the current one.
     * @param staleKeys Params a panel no longer has, to take out as well.
     */
    #write(mode: 'push' | 'replace', staleKeys: readonly string[] = []): void {
        const current = this.#serializer.parse(this.#location.path(true));
        const params: Params = { ...current.queryParams };
        const panelKeys = this.#entries.flatMap((entry) => Object.keys(entry.params));

        for (const key of [...PANEL_PARAMS, ...panelKeys, ...staleKeys]) {
            delete params[key];
        }

        Object.assign(params, this.#front()?.params);

        const next = this.#serializer.serialize(
            new UrlTree(current.root, params, current.fragment)
        );

        if (next === this.#serializer.serialize(current)) {
            return;
        }

        if (mode === 'push') {
            this.#location.go(next);
        } else {
            this.#location.replaceState(next);
        }
    }
}
