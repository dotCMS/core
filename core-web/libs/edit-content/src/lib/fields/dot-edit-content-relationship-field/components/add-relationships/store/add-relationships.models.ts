import {
    ComponentStatus,
    DotContentDriveBrowseItem,
    DotCMSContentlet
} from '@dotcms/dotcms-models';

import { SelectionMode } from '../../../models/relationship.models';

/**
 * The filters the editor can see and change in this dialog, by their known keys.
 *
 * Shaped like `DotAssetPickerFilters` and `DotContentDriveFilters` for the same reason those match
 * each other: one shared chip has to write into any of them without knowing which surface it is on.
 *
 * Note what is **absent**: the target content type. It is a caller restriction, not a filter — the
 * editor must never be able to widen the result set past the relationship's own type — so it lives
 * in the config and never in this bag. Keeping it out is what makes that structural rather than a
 * convention the next chip could break.
 */
export interface DotAddRelationshipsFilters {
    /** Free-text search term. */
    title?: string;
    /** Selected language ids. */
    languageId?: string[];
    /** Site / folder scope, as the shared chip expresses it. */
    site?: string[];

    [key: string]: string | string[] | undefined;
}

/**
 * Where the result list currently is.
 *
 * Only a page number and a size: the **cursors live in {@link DotAddRelationshipsState.pages}**, one
 * per visited page. A single "current cursor" cannot page backwards — the endpoint hands back the
 * bookmark for the *next* page, so replaying it to reach the previous one asks for the page you just
 * left. Mirrors the AssetPicker's `pages` bookmarks, for the same reason.
 */
export interface DotAddRelationshipsPage {
    /** 1-based, for display and for proving that a filter change reset the list. */
    number: number;
    limit: number;
}

/**
 * Cursor bookmark recorded **after** loading a page.
 *
 * `pages[N]` therefore answers two questions about page N: where the page after it starts, and
 * whether there is one.
 */
export interface DotAddRelationshipsBookmark {
    contentCursor: number;
    hasMoreContent: boolean;
}

export interface DotAddRelationshipsSort {
    field: string;
    order: 'asc' | 'desc';
}

export interface AddRelationshipsState {
    /** The relationship's target content type. Fixed for the life of the dialog. */
    contentTypeId: string;

    /**
     * Whether the editor may hold one item or several, from the field's cardinality.
     *
     * In `single` the selection is a slot: a second pick replaces the first rather than adding.
     */
    selectionMode: SelectionMode;

    /**
     * The drive-search path the results are scoped to, e.g. `//demo.dotcms.com/`.
     *
     * Not optional in practice: `/drive/search` resolves its host and folder from this and rejects
     * an unparseable value outright, so an empty one fails the request rather than widening it.
     *
     * **Scope, not a filter.** It lives here rather than in {@link filters} for the same reason
     * Content Drive keeps its browsed folder outside them: "Clear all" returns the filters to their
     * defaults and deliberately leaves the editor where they were browsing. Routing it through the
     * filter bag would also hide it from `buildRequest`, which reads named keys only — which is
     * exactly how the chip came to look like it worked while changing nothing.
     */
    assetPath: string;

    /** What the site chip shows — the hostname, or `hostname/path` inside a folder. */
    scopeLabel: string;

    /**
     * The scope the dialog opened on, so "reset" can return to it.
     *
     * Needed because the scope is **not** a filter: clearing filters leaves the editor where they
     * were browsing, which is right — but it means a scope with no results (Shared Assets, say)
     * would otherwise be a dead end with no control offering a way back.
     */
    defaultAssetPath: string;
    defaultScopeLabel: string;

    /** The current page of results. Server-paged, so this is a window, never the whole set. */
    items: DotContentDriveBrowseItem[];

    /**
     * The editor's selection — **accumulated**, not a reading of the rows on screen.
     *
     * Keyed by **identifier** rather than inode: identifiers are stable across saves and inodes are
     * not, and `RelationshipFieldStore.refreshItem` already depends on exactly that.
     *
     * Holding the whole contentlet rather than the id is what lets confirmation return an item that
     * the current page — or any page, or any search — does not contain. With server-side paging
     * that is not an optimisation, it is the difference between keeping the editor's relationships
     * and silently dropping the ones they had scrolled past.
     *
     * Seeded from `AddRelationshipsInput.selected`. **Never rebuilt from a search response.**
     */
    selection: Map<string, DotCMSContentlet>;

    /**
     * Inodes still waiting to be matched against a result, for the degraded pre-selection path
     * (`AddRelationshipsInput.selectedInodes`). Each is dropped once its row appears and joins
     * {@link selection} keyed by identifier like everything else.
     *
     * Empty for the edit-time caller, which hands over contentlets and needs no resolution.
     */
    pendingInodes: Set<string>;

    /** Read and written only through `DOT_FILTER_FACADE`; nothing else touches this. */
    filters: DotAddRelationshipsFilters;

    /**
     * The filters the dialog opened with — what "Clear all" returns to.
     *
     * Clearing to an **empty** set would strand the editor in an unfiltered library, which is not
     * what the control promises; every surface clears to its own defaults instead.
     */
    defaultFilters: DotAddRelationshipsFilters;

    page: DotAddRelationshipsPage;

    /**
     * Cursor bookmarks by page number, written as each page loads.
     *
     * Cleared whenever the result set changes shape — a filter, a search, a sort or a new scope —
     * because a cursor taken against one result set means nothing in another.
     */
    pages: Record<number, DotAddRelationshipsBookmark>;

    sort: DotAddRelationshipsSort;

    /**
     * Identifiers already related to a *different* parent through a relationship that permits only
     * one. Listed, never selectable.
     *
     * Empty when the caller supplied no parent context — filter-time has none, and a partial
     * lookup would be worse than no lookup.
     */
    constrainedIdentifiers: Set<string>;

    /**
     * Whether the list shows every result or only what the editor has picked.
     *
     * The selected view reads from {@link selection}, never from {@link items} — otherwise it could
     * only ever show the picks that happen to be on the current page.
     */
    viewMode: 'all' | 'selected';

    status: ComponentStatus;

    /** Translation key of the last failure, or null. */
    errorMessage: string | null;
}
