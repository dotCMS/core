import { from, Observable, of } from 'rxjs';

import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Service } from '@angular/core';

import { catchError, map, mergeMap, switchMap, tap, toArray } from 'rxjs/operators';

import { DotCMSAPIResponse } from '@dotcms/dotcms-models';

/** One row of `GET /api/v1/users/filter`; only what a chip option needs is declared. */
export interface DotUserSearchRow {
    userId: string;
    fullName?: string;
    firstName?: string;
    lastName?: string;
    emailAddress?: string;
}

export interface DotUserSearchParams {
    /** Free text, matched against id, first name, last name, email and full name. */
    filter: string;
    /** 1-based. The endpoint declares a default of 0 but coerces `page <= 0` to the first page. */
    page: number;
    perPage: number;
    /** Sent only when given. Without one the endpoint's order is unspecified. */
    orderBy?: string;
    direction?: 'ASC' | 'DESC';
}

const USERS_FILTER_URL = '/api/v1/users/filter';

/** Rows fetched per page while hunting one exact id. */
const EXACT_MATCH_WINDOW = 20;

/**
 * Pages walked at most before giving up on one id.
 *
 * `query` matches ids as a substring, so asking for `dotcms.org.1` returns every longer id
 * containing it and nothing guarantees the exact one comes first. A single page of twenty was the
 * original mitigation and it is not a guarantee: past twenty near-misses the exact id falls off
 * the page and a real person renders as a raw id after a reload.
 *
 * So the search pages. It is still bounded, because the alternative is letting one pathological id
 * walk a directory of any size on every page load — and an unresolved id is a cosmetic loss, not a
 * broken filter: matching is by id and needs no label.
 */
export const MAX_RESOLVE_PAGES = 5;

/** In-flight resolutions allowed at once. See the note in `resolveNames`. */
const RESOLVE_CONCURRENCY = 3;

/**
 * Searches the dotCMS user directory.
 *
 * Lives here rather than in a portlet because more than one surface needs it: the Users and Roles
 * portlets each hold a copy of this call today, and neither publishes it. Consolidating those two
 * onto this service is deliberately out of scope — see
 * `specs/37307-experiments-list-filters-pagination/contracts/user-directory-search.md`.
 *
 * Requires only a back-end user, which matters: the single-user lookup
 * (`GET /v1/users/{userId}`) demands administrator rights or both the Roles and Users portlets and
 * refuses everyone else, so it must not be used to resolve a name. Doing that would give a filter
 * that labels correctly when a person is picked and breaks on reload, for non-administrators only.
 *
 * **What every caller inherits, and should decide about deliberately.** This endpoint applies no
 * permission filtering to the rows it returns. `UserPaginator` reaches
 * `UserAPI.getUsersByName(filter, roles, start, limit, filteringParams)` — the overload that takes
 * no requesting user — and the requester is used only to drop itself from the list. An overload
 * that *does* take a requesting user exists and is not the one used. So any back-end user sees the
 * whole directory: names, and the email addresses the rows carry.
 *
 * That is the pre-existing behaviour of the Users and Roles portlets, which are administrative
 * screens. A caller on a screen reachable by a lower-privileged editor is widening *where* the
 * directory is exposed, not whether it is — and should say out loud that it accepts that. The
 * narrowing tool if it does not: the endpoint takes a `roleKey`, so candidates can be limited to
 * holders of a given role (e.g. `DOTCMS_BACK_END_USER`).
 *
 * Anonymous and default users are excluded unless asked for: `includeanonymous` and
 * `includedefault` both default to false and this service does not send them, so the list is real
 * people rather than system accounts.
 *
 * **Not auto-provided.** `@Service()` alone would register this in the root injector; with
 * `autoProvided: false` the class keeps its DI metadata and nothing else, so a `providers` list has
 * to name it. `DotUserFilterComponent` does. There is nothing here worth sharing across the
 * application — no cache, no subscription, no state, just a shaped call — so a root singleton only
 * put it in every injector that never asks for it, and its lifetime now matches the control that
 * does.
 */
@Service({ autoProvided: false })
export class DotUserSearchService {
    readonly #http = inject(HttpClient);

    /**
     * One page of the directory, as the raw envelope.
     *
     * Deliberately unmapped: `pagination.totalEntries` is what tells a caller whether another page
     * exists, and a service that returned a bare array would make infinite scroll impossible to
     * stop. Failures propagate — the caller decides how to report them.
     */
    searchPage({
        filter,
        page,
        perPage,
        orderBy,
        direction
    }: DotUserSearchParams): Observable<DotCMSAPIResponse<DotUserSearchRow[]>> {
        let params = new HttpParams()
            .set('query', filter ?? '')
            .set('page', String(page))
            .set('per_page', String(perPage));

        if (orderBy) {
            params = params.set('orderby', orderBy);
        }

        if (direction) {
            params = params.set('direction', direction);
        }

        return this.#http
            .get<DotCMSAPIResponse<DotUserSearchRow[]>>(USERS_FILTER_URL, { params })
            .pipe(tap((response) => this.#remember(response?.entity ?? [])));
    }

    /**
     * Display names for the given ids, keyed by id.
     *
     * One request per id: the endpoint has no bulk-by-ids parameter, so this is linear in the
     * number of ids by necessity rather than by choice.
     *
     * Every id always comes back with a value. An id that cannot be resolved — no match, no name,
     * or a failed request — maps to itself, so a caller can render the result without checking for
     * gaps and a failure never drops a selection.
     */
    resolveNames(userIds: string[]): Observable<Record<string, string>> {
        const unknown = userIds.filter((userId) => !this.#nameById.has(userId));

        if (!unknown.length) {
            return of(this.#fromCache(userIds));
        }

        return from(unknown)
            .pipe(
                // Bounded, not `forkJoin`. There is no bulk-by-ids parameter, so this is one
                // search per id by necessity — but firing all of them at once turns a selection
                // of fifteen people into fifteen simultaneous connections on page load. A short
                // queue costs a little latency and spares the browser and the server the burst.
                mergeMap(
                    (userId) =>
                        this.#resolveOne(userId).pipe(
                            tap((name) => this.#nameById.set(userId, name))
                        ),
                    RESOLVE_CONCURRENCY
                )
            )
            .pipe(
                toArray(),
                map(() => this.#fromCache(userIds))
            );
    }

    /**
     * The display name of one id, paging until the exact match is found or the search is spent.
     *
     * Ordered explicitly: without one the endpoint's order is unspecified, and a set that shifts
     * between two requests can serve a row twice and never serve another at all — which for a hunt
     * that pages is the difference between finding the id and silently missing it.
     *
     * Every exit returns the id itself rather than failing. A failed request, an exhausted
     * directory, a spent page budget and a row with no name are all the same outcome to a caller:
     * no label. The filter still matches, because matching is by id.
     */
    #resolveOne(userId: string, page = 1): Observable<string> {
        return this.searchPage({
            filter: userId,
            page,
            perPage: EXACT_MATCH_WINDOW,
            orderBy: 'userId',
            direction: 'ASC'
        }).pipe(
            switchMap((response) => {
                const rows = response?.entity ?? [];
                const exact = rows.find((row) => row.userId === userId);

                if (exact) {
                    return of(dotUserDisplayName(exact));
                }

                const served = page * EXACT_MATCH_WINDOW;
                const total = response?.pagination?.totalEntries ?? 0;

                return page >= MAX_RESOLVE_PAGES || served >= total
                    ? of(userId)
                    : this.#resolveOne(userId, page + 1);
            }),
            catchError(() => of(userId))
        );
    }

    /**
     * Names every page has already revealed, so a selection made in the list — or a page revisited
     * — costs nothing to label.
     *
     * Deliberately unbounded and never invalidated: it holds only ids and display names for the
     * lifetime of the tab, and a renamed user is a cosmetic staleness nobody has asked us to
     * chase. If it ever needs a ceiling, that is a change of mind about this comment, not a bug.
     */
    readonly #nameById = new Map<string, string>();

    #remember(rows: DotUserSearchRow[]): void {
        rows.forEach((row) => this.#nameById.set(row.userId, dotUserDisplayName(row)));
    }

    #fromCache(userIds: string[]): Record<string, string> {
        return userIds.reduce<Record<string, string>>((resolved, userId) => {
            resolved[userId] = this.#nameById.get(userId) ?? userId;

            return resolved;
        }, {});
    }
}

/**
 * How a dotCMS user is named on screen.
 *
 * The fallback chain is not defensive padding: `fullName` is genuinely blank on legacy and
 * partially-imported accounts, and without the chain those users render as a raw id — which is
 * what the Users portlet's replacement picker found and worked around locally. That rule belongs
 * next to the call that produces the rows, so every surface names the same person the same way.
 *
 * Returns the id as the last resort, so the result is never blank.
 */
export function dotUserDisplayName(row: DotUserSearchRow): string {
    const candidates = [
        row.fullName,
        [row.firstName, row.lastName].filter(Boolean).join(' '),
        row.emailAddress
    ];

    return candidates.map((candidate) => (candidate ?? '').trim()).find(Boolean) || row.userId;
}
