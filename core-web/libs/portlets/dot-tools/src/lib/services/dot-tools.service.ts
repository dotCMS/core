import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map } from 'rxjs/operators';

import {
    DotToolsCatalogEntry,
    DotToolsCustomToolConfig,
    DotToolsSection,
    DotToolsSectionForm,
    DotToolsToolForm
} from '../models/dot-tools.models';

/**
 * Every dotCMS REST v1 response is wrapped as `{ entity, errors, messages, ... }`
 * and the useful payload sits on `entity`. We only ever read `entity`.
 */
interface ResponseEntity<T> {
    entity: T;
}

/**
 * Tools portlet HTTP.
 *
 * Catalog + custom-tool endpoints (all methods except the six section ones)
 * are pinned by PR #37678 — merged, or about to be. Section endpoints are
 * pinned by the spec on PR #37645; the implementation PR will follow.
 *
 * Everything here is a plain HTTP call — no mocked bodies. The endpoints not
 * yet on the running build will fail with 404 or the endpoint's own error,
 * which is what we want: fail states surface early instead of hiding behind
 * fixtures that pretend the write succeeded.
 */
@Injectable({ providedIn: 'root' })
export class DotToolsService {
    readonly #http = inject(HttpClient);

    // ----- Sections (spec: PR #37645) ------------------------------------

    getSections(): Observable<DotToolsSection[]> {
        return this.#http
            .get<ResponseEntity<DotToolsSection[]>>('/api/v1/layouts')
            .pipe(map((res) => res.entity));
    }

    createSection(form: DotToolsSectionForm): Observable<DotToolsSection> {
        return this.#http
            .post<ResponseEntity<DotToolsSection>>('/api/v1/layouts', form)
            .pipe(map((res) => res.entity));
    }

    updateSection(id: string, form: DotToolsSectionForm): Observable<DotToolsSection> {
        return this.#http
            .put<ResponseEntity<DotToolsSection>>(`/api/v1/layouts/${encodeURIComponent(id)}`, form)
            .pipe(map((res) => res.entity));
    }

    /** Delete, reorder, and setSectionTools all return the full section list. */
    deleteSection(id: string): Observable<DotToolsSection[]> {
        return this.#http
            .delete<ResponseEntity<DotToolsSection[]>>(`/api/v1/layouts/${encodeURIComponent(id)}`)
            .pipe(map((res) => res.entity));
    }

    reorderSections(orderedIds: string[]): Observable<DotToolsSection[]> {
        return this.#http
            .put<ResponseEntity<DotToolsSection[]>>('/api/v1/layouts/_reorder', orderedIds)
            .pipe(map((res) => res.entity));
    }

    setSectionTools(sectionId: string, portletIds: string[]): Observable<DotToolsSection[]> {
        return this.#http
            .put<
                ResponseEntity<DotToolsSection[]>
            >(`/api/v1/layouts/${encodeURIComponent(sectionId)}/portlets`, portletIds)
            .pipe(map((res) => res.entity));
    }

    // ----- Catalog + custom tool (PR #37678) -----------------------------

    getCatalog(): Observable<DotToolsCatalogEntry[]> {
        return this.#http
            .get<ResponseEntity<DotToolsCatalogEntry[]>>('/api/v1/portlet/_catalog')
            .pipe(map((res) => res.entity));
    }

    getCustomTool(portletId: string): Observable<DotToolsCustomToolConfig> {
        return this.#http
            .get<
                ResponseEntity<DotToolsCustomToolConfig>
            >(`/api/v1/portlet/custom/${encodeURIComponent(portletId)}`)
            .pipe(map((res) => res.entity));
    }

    createCustomTool(form: DotToolsToolForm): Observable<DotToolsCatalogEntry> {
        return this.#http
            .post<ResponseEntity<DotToolsCustomToolConfig>>('/api/v1/portlet/custom', form)
            .pipe(map((res) => this.#customToolToCatalogEntry(res.entity)));
    }

    updateCustomTool(form: DotToolsToolForm): Observable<DotToolsCatalogEntry> {
        return this.#http
            .put<ResponseEntity<DotToolsCustomToolConfig>>('/api/v1/portlet/custom', form)
            .pipe(map((res) => this.#customToolToCatalogEntry(res.entity)));
    }

    deleteCustomTool(portletId: string): Observable<void> {
        return this.#http.delete<void>(`/api/v1/portlet/custom/${encodeURIComponent(portletId)}`);
    }

    /**
     * Create and update return the full custom-tool config; the store only
     * needs a catalog entry to slot into `catalog[]`. Custom tools created
     * or edited through this portlet are always custom, so `isCustom: true`
     * is a hard-coded truth rather than something we need to re-fetch.
     */
    #customToolToCatalogEntry(config: DotToolsCustomToolConfig): DotToolsCatalogEntry {
        return { id: config.portletId, title: config.portletName, isCustom: true };
    }
}
