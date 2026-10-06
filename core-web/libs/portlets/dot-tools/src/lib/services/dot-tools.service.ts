import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map, switchMap } from 'rxjs/operators';

import {
    DotToolsCatalogEntry,
    DotToolsCustomToolConfig,
    DotToolsDataViewMode,
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
 * Tools portlet HTTP. Catalog + custom-tool endpoints come from PR #37678,
 * section endpoints from PR #37729 — both merged.
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
            .put<ResponseEntity<DotToolsSection[]>>('/api/v1/layouts/_reorder', {
                layoutIds: orderedIds
            })
            .pipe(map((res) => res.entity));
    }

    setSectionTools(sectionId: string, portletIds: string[]): Observable<DotToolsSection[]> {
        return this.#http
            .put<
                ResponseEntity<DotToolsSection[]>
            >(`/api/v1/layouts/${encodeURIComponent(sectionId)}/portlets`, { portletIds })
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
            .post<
                ResponseEntity<{ portlet: string }>
            >('/api/v1/portlet/custom', this.#toCustomPortletForm(form))
            .pipe(switchMap((res) => this.#catalogEntryFor(res.entity.portlet)));
    }

    updateCustomTool(form: DotToolsToolForm): Observable<DotToolsCatalogEntry> {
        return this.#http
            .put<
                ResponseEntity<{ portlet: string }>
            >('/api/v1/portlet/custom', this.#toCustomPortletForm(form))
            .pipe(switchMap((res) => this.#catalogEntryFor(res.entity.portlet)));
    }

    deleteCustomTool(portletId: string): Observable<void> {
        return this.#http.delete<void>(`/api/v1/portlet/custom/${encodeURIComponent(portletId)}`);
    }

    /**
     * `CustomPortletForm` on the backend declares `baseTypes` / `contentTypes`
     * as comma-separated `String`s (see
     * dotCMS/.../v1/portlet/CustomPortletForm.java). Sending the form's arrays
     * verbatim makes Jackson reject the body.
     */
    #toCustomPortletForm(form: DotToolsToolForm): {
        portletId: string;
        portletName: string;
        baseTypes: string;
        contentTypes: string;
        dataViewMode: DotToolsDataViewMode;
    } {
        return {
            portletId: form.portletId,
            portletName: form.portletName,
            baseTypes: form.baseTypes.join(','),
            contentTypes: form.contentTypes.join(','),
            dataViewMode: form.dataViewMode
        };
    }

    /**
     * `POST` / `PUT /v1/portlet/custom` return only `{ entity: { portlet: id } }`
     * (`PortletResource.java:230` / `:387`), not the full config. We fetch the
     * config so the catalog entry carries the resolved title and id.
     */
    #catalogEntryFor(portletId: string): Observable<DotToolsCatalogEntry> {
        return this.getCustomTool(portletId).pipe(
            map((config) => ({
                id: config.portletId,
                title: config.portletName,
                isCustom: true
            }))
        );
    }
}
