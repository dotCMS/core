import { inject, Injectable } from '@angular/core';
import { ActivatedRoute, Params, Router } from '@angular/router';

import { DotExperimentsPanelStore } from '@dotcms/portlets/dot-experiments/data-access';

import {
    CONFIGURATION_SEGMENT,
    EXPERIMENTS_URL,
    NEW_EXPERIMENT_SEGMENT
} from '../shared/constants';
import {
    configureCommandsOf,
    listReturnParams,
    resultsCommandsOf
} from '../util/dot-experiments-list.util';

/**
 * Where the experiments screens go, and the one place that knows how they get there (#37478).
 *
 * Named for what it is. `DotExperimentsPanelStore` holds the panel's address; this is what moves
 * between the addresses — Angular's `Router` for the portlet, that store for the panel. Consumers
 * inject it as `#experimentsRouter` so the two never read as the same thing at a glance.
 *
 * The same four destinations exist in both worlds and are reached two different ways. The portlet
 * navigates: each screen is a route, and the address carries the narrowing so the next screen can
 * find its way back. The panel changes a view: there are no routes, and the way back is the panel
 * itself. Every screen used to carry that fork inline — sixteen copies of
 * `if (this.#panel) { … return; } this.#router.navigate(…)`, in eleven files — so a destination
 * could be taught to the panel in one place and left navigating in another, which is exactly how
 * this feature kept ejecting the editor from doors nobody had looked at.
 *
 * The fork lives here now, once. When the entry-point switch is retired and the panel is the only
 * world, it is deleted here and nowhere else.
 *
 * **Not `providedIn: 'root'`.** {@link DotExperimentsPanelStore} is provided by the UVE route, so
 * only an injector beneath it can see it; a root-provided service would always resolve `null` and
 * quietly navigate the editor away. Each screen provides this, and its cards and headers inherit
 * the same instance.
 *
 * What is deliberately *not* here: leaving for a variant. That is a trip out of the experiments
 * screens rather than a move between them, it suspends the panel on the way, and the return is the
 * toolbar's. Folding it in would make this the place where two unrelated ideas meet.
 */
@Injectable()
export class DotExperimentsRouter {
    readonly #panel = inject(DotExperimentsPanelStore, { optional: true });
    readonly #router = inject(Router);
    readonly #route = inject(ActivatedRoute);

    /** Whether the screens are rendering inside the panel rather than as the full-screen portlet. */
    get inPanel(): boolean {
        return !!this.#panel;
    }

    /**
     * The configuration of an existing experiment.
     *
     * @param queryParams the narrowing to carry, when the caller knows a better one than the
     * address does — the list holds its own filter in its store rather than on the route it is
     * about to leave.
     */
    toConfiguration(experimentId: string, queryParams?: Params): void {
        if (this.#panel) {
            this.#panel.showConfigure(experimentId);

            return;
        }

        this.#router.navigate(configureCommandsOf(experimentId), {
            queryParams: queryParams ?? this.#returnParams()
        });
    }

    /** An experiment's results. */
    toResults(experimentId: string, queryParams?: Params): void {
        if (this.#panel) {
            this.#panel.showResults(experimentId);

            return;
        }

        this.#router.navigate(resultsCommandsOf(experimentId), {
            queryParams: queryParams ?? this.#returnParams()
        });
    }

    /** The list of experiments the screen was opened from. */
    toList(queryParams?: Params): void {
        if (this.#panel) {
            this.#panel.backToList();

            return;
        }

        this.#router.navigate([EXPERIMENTS_URL], {
            queryParams: queryParams ?? this.#returnParams()
        });
    }

    /** The creation screen: the configuration with nothing created yet. */
    toCreate(queryParams?: Params): void {
        if (this.#panel) {
            this.#panel.showCreate();

            return;
        }

        this.#router.navigate([EXPERIMENTS_URL, NEW_EXPERIMENT_SEGMENT], {
            queryParams: queryParams ?? this.#returnParams()
        });
    }

    /**
     * Where an experiment goes the moment it exists.
     *
     * Its own method because the portlet's answer is not a destination but a **correction**: it
     * swaps `/experiments/new` for the new experiment's URL with `replaceUrl`, so Back leaves the
     * screen instead of returning to a creation form for something that already exists. The panel
     * has no URL to swap and no history entry to keep out of, so it simply shows it.
     */
    afterCreated(experimentId: string): void {
        if (this.#panel) {
            this.#panel.showConfigure(experimentId);

            return;
        }

        this.#router.navigate(['..', experimentId, CONFIGURATION_SEGMENT], {
            relativeTo: this.#route,
            replaceUrl: true,
            queryParamsHandling: 'preserve'
        });
    }

    /** The narrowing the screen arrived on, so the next one can find its way back (FR-021c). */
    #returnParams(): Params {
        return listReturnParams(this.#route.snapshot.queryParams);
    }
}
