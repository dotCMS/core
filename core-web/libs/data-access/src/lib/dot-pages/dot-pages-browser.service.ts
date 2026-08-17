import { Observable } from 'rxjs';

import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map } from 'rxjs/operators';

import { DotCMSResponse } from '@dotcms/dotcms-models';

import {
    DotPageBrowserContentlet,
    DotPageBrowserPage,
    DotPageBrowserState,
    DotPageLockInfo,
    DotPagesBrowserSearchParams
} from './dot-pages-browser.models';

const PAGE_SEARCH_URL = '/api/v1/page/search';
const ES_SEARCH_URL = '/api/es/search';
const PAGE_BASE_TYPE_QUERY = '+basetype:5';
const SITE_ROOT_PATH = '/';

/** Response shape of `POST /api/es/search`. */
interface DotESSearchResponse<T> {
    contentlets: T;
}

/**
 * Page-browsing data access for page pickers that are not nested under a per-page route:
 * folder navigation, page listing and a page's lock state, each resolved from a single
 * request so a picker never needs the full page-render pipeline.
 */
@Injectable()
export class DotPagesBrowserService {
    readonly #http = inject(HttpClient);

    /**
     * Lists the pages matching a folder path, optionally narrowed by a text term.
     *
     * `GET /api/v1/page/search` filters by a single `path` substring (site scoping is expressed
     * as a `//hostname/...` prefix) and has no free-text parameter, so `query` is matched
     * against the returned rows' title and url.
     *
     * @param params - Site hostname, folder path, text term and version filters
     * @returns Observable of page rows with their publication state resolved
     */
    searchPages(params: DotPagesBrowserSearchParams = {}): Observable<DotPageBrowserPage[]> {
        const httpParams = new HttpParams()
            .set('path', this.#buildSearchPath(params))
            .set('live', String(params.live ?? false))
            .set('onlyLiveSites', String(params.onlyLiveSites ?? false));

        return this.#http
            .get<
                DotCMSResponse<DotPageBrowserContentlet[]>
            >(PAGE_SEARCH_URL, { params: httpParams })
            .pipe(
                map((response) => response?.entity ?? []),
                map((contentlets) => contentlets.map((page) => this.#toPageRow(page))),
                map((pages) => this.#filterByQuery(pages, params.query))
            );
    }

    /**
     * Reads the lock state of a single page by identifier.
     *
     * Resolved with one identifier-scoped content search — the lightest available call, since
     * no page endpoint exposes lock metadata on its own and rendering the page is far more
     * expensive. Nothing user-relative is computed here: compare {@link DotPageLockInfo.lockedBy}
     * against the current user id (`DotCurrentUserService` / `LoginService`) to obtain
     * `lockedByAnotherUser`.
     *
     * @param pageId - Identifier of the page
     * @returns Observable of the page's lock state; unlocked when the page cannot be found
     */
    getPageLockState(pageId: string): Observable<DotPageLockInfo> {
        const body = {
            query: {
                query_string: {
                    query: `${PAGE_BASE_TYPE_QUERY} +identifier:${pageId}`
                }
            },
            size: 1
        };

        return this.#http
            .post<DotESSearchResponse<DotPageBrowserContentlet[]>>(ES_SEARCH_URL, body)
            .pipe(map((response) => this.#toLockInfo(response?.contentlets?.[0])));
    }

    #buildSearchPath({ hostname, path }: DotPagesBrowserSearchParams): string {
        const folderPath = path?.startsWith(SITE_ROOT_PATH)
            ? path
            : `${SITE_ROOT_PATH}${path ?? ''}`;

        return hostname ? `//${hostname}${folderPath}` : folderPath;
    }

    #toPageRow(page: DotPageBrowserContentlet): DotPageBrowserPage {
        const url = page.url ?? '';

        return {
            identifier: page.identifier,
            inode: page.inode,
            title: page.title || url,
            url,
            path: page.path ?? url,
            hostname: page.hostName,
            hostId: page.host,
            templateId: page.template ?? '',
            modDate: page.modDate ?? '',
            languageId: page.languageId,
            state: this.#toPageState(page)
        };
    }

    #toPageState(page: DotPageBrowserContentlet): DotPageBrowserState {
        if (page.archived) {
            return DotPageBrowserState.ARCHIVED;
        }

        if (page.live) {
            return page.working === false
                ? DotPageBrowserState.CHANGED
                : DotPageBrowserState.PUBLISHED;
        }

        return page.hasLiveVersion ? DotPageBrowserState.CHANGED : DotPageBrowserState.DRAFT;
    }

    #toLockInfo(page: DotPageBrowserContentlet | undefined): DotPageLockInfo {
        const lockedBy = this.#toLockedByUserId(page?.lockedBy);

        if (!lockedBy) {
            return { locked: false };
        }

        return {
            locked: true,
            lockedBy,
            ...(page?.lockedByName ? { lockedByName: page.lockedByName } : {})
        };
    }

    #toLockedByUserId(lockedBy: DotPageBrowserContentlet['lockedBy']): string | undefined {
        if (typeof lockedBy === 'string') {
            return lockedBy || undefined;
        }

        return lockedBy?.userId || undefined;
    }

    #filterByQuery(pages: DotPageBrowserPage[], query?: string): DotPageBrowserPage[] {
        const term = query?.trim().toLowerCase();

        if (!term) {
            return pages;
        }

        return pages.filter(
            ({ title, url }) =>
                title.toLowerCase().includes(term) || url.toLowerCase().includes(term)
        );
    }
}
