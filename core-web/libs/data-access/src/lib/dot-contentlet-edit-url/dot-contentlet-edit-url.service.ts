import { Observable, of } from 'rxjs';

import { Injectable, inject } from '@angular/core';

import { catchError, map } from 'rxjs/operators';

import {
    DotCMSBaseTypesContentTypes,
    DotCMSContentlet,
    DotCMSContentType,
    FeaturedFlags
} from '@dotcms/dotcms-models';

import { DotContentTypeService } from '../dot-content-type/dot-content-type.service';

/** The two content editors: the new Angular editor, or the legacy (JSP) one. */
export type DotContentEditor = 'new' | 'legacy';

/** Which editor a content type uses, and the content type the decision was read from. */
export interface DotContentEditorChoice {
    editor: DotContentEditor;
    /** The resolved content type, or `null` when the lookup failed (then `editor` is `'legacy'`). */
    contentType: DotCMSContentType | null;
}

/**
 * Canonical resolver for "which editor opens this content type?" ({@link resolveEditor}) and
 * "where in dotAdmin should I open this contentlet for editing?" ({@link resolveEditUrl}).
 *
 * This service exists to consolidate logic that has been re-implemented inline at multiple
 * call sites across the admin (Query Tool, Content Drive's `#editContentlet`, the legacy and
 * new block-editor "edit contentlet" actions). Adopting this service means future changes —
 * editor URL format updates, feature-flag renames, new fallback rules — land in one place
 * instead of drifting across copies.
 *
 * ## What it resolves
 *
 * {@link resolveEditor} answers the editor decision alone: `'new'` when the content type's
 * metadata has `CONTENT_EDITOR2_ENABLED`, `'legacy'` otherwise, with the content type it read.
 * The side-panel opener in `@dotcms/edit-content` uses it to pick the panel (#38303).
 *
 * {@link resolveEditUrl} is built on it. Given a contentlet, returns the dotAdmin hash route that should open it for editing.
 * Three shapes, in priority order:
 *
 * 1. **HTMLPAGE** (`baseType === HTMLPAGE`)
 *    → `/dotAdmin/#/edit-page/content?url=<url>&language_id=<lang>&mId=edit`
 *    Uses the dedicated page editor route. Skips the content-type metadata lookup entirely
 *    because the routing decision is determined by `baseType`, not by per-type feature flags.
 *
 * 2. **Contentlet on a content type with `CONTENT_EDITOR2_ENABLED`**
 *    → `/dotAdmin/#/content/<inode>`
 *    The new dotCMS content editor.
 *
 * 3. **Contentlet on a legacy content type**
 *    → `/dotAdmin/#/c/content/<inode>`
 *    The legacy content editor (Dijit-era form).
 *
 * The caller decides what to do with the URL (open in new tab, `Router.navigate`, etc.) —
 * this service only resolves "what is the URL?".
 *
 * ## Caching
 *
 * The editor choice is cached per content type for the lifetime of the application (the
 * service is `providedIn: 'root'`), one cache for both methods. A list view rendering 50
 * contentlets of the same content type hits `/api/v1/contenttype/id/{type}` once, not 50 times.
 * A failed lookup is not cached, so the next call asks the server again.
 *
 * **Cache invalidation caveat:** if an admin toggles `CONTENT_EDITOR2_ENABLED` on a content
 * type during the same session, cached entries go stale until a full page reload. This
 * matches the behavior of the prior inline implementations and is acceptable because the
 * flag is rarely toggled at runtime.
 *
 * ## Error fallback
 *
 * If the content-type lookup fails (4xx/5xx, network), the resolver chooses the **legacy**
 * editor (and its URL) rather than propagating the error. Rationale: the legacy editor handles every
 * content type, so falling back keeps the user's edit flow working even when metadata
 * reads transiently fail. The error is intentionally swallowed — callers that need to
 * react to the failure should query `DotContentTypeService.getContentType()` directly.
 *
 * ## Existing duplicates (TODO migrations)
 *
 * Three older implementations of this pattern still exist and should migrate to this
 * service when their owners next touch the surrounding code:
 *
 * - `libs/new-block-editor/src/lib/editor/services/contentlet-edit-url.service.ts`
 *   (component-scoped, no HTMLPAGE branch)
 * - `libs/portlets/dot-content-drive/.../dot-content-drive-navigation.service.ts#editContentlet`
 *   (calls `Router.navigate` directly; would consume `resolveEditUrl(...)` and pass the
 *   result to `Router.navigateByUrl`)
 * - `libs/block-editor/src/lib/elements/dot-bubble-menu/dot-bubble-menu.component.ts`
 *   (inline, no cache, legacy editor only)
 *
 * @example
 * ```ts
 * readonly #editUrl = inject(DotContentletEditUrlService);
 *
 * onEditClick(contentlet: DotCMSContentlet): void {
 *   this.#editUrl.resolveEditUrl(contentlet).subscribe((url) => window.open(url, '_blank'));
 * }
 * ```
 */
@Injectable({ providedIn: 'root' })
export class DotContentletEditUrlService {
    readonly #contentTypeService = inject(DotContentTypeService);

    // Cached per content-type variable or id. Only successful lookups land here.
    readonly #editorCache = new Map<string, DotContentEditorChoice>();

    /**
     * Resolves which editor opens content of a type: `'new'` when the type's metadata has
     * `CONTENT_EDITOR2_ENABLED`, `'legacy'` otherwise. A failed lookup falls back to `'legacy'`
     * with no content type and is not cached, so a transient error does not stick for the session.
     *
     * @param contentTypeVariableOrId The content type's variable or id.
     * @returns An observable that emits the choice once and completes.
     */
    resolveEditor(contentTypeVariableOrId: string): Observable<DotContentEditorChoice> {
        const cached = this.#editorCache.get(contentTypeVariableOrId);
        if (cached) return of(cached);

        return this.#contentTypeService.getContentType(contentTypeVariableOrId).pipe(
            map((contentType) => {
                const choice: DotContentEditorChoice = {
                    editor: contentType?.metadata?.[
                        FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED
                    ]
                        ? 'new'
                        : 'legacy',
                    contentType
                };
                this.#editorCache.set(contentTypeVariableOrId, choice);

                return choice;
            }),
            catchError(() => of<DotContentEditorChoice>({ editor: 'legacy', contentType: null }))
        );
    }

    /**
     * Resolves the dotAdmin URL to open `contentlet` for editing. See the class JSDoc
     * for the routing rules, caching behavior, and error fallback semantics.
     *
     * @param contentlet The contentlet the user wants to edit. Must have at least
     *                   `inode`, `contentType`, and `baseType` populated; HTMLPAGE
     *                   contentlets also need `url` (or `urlMap`) and `languageId`.
     * @returns An observable that emits exactly one URL string and completes.
     */
    resolveEditUrl(contentlet: DotCMSContentlet): Observable<string> {
        const pageUrl = buildPageEditUrl(contentlet);
        if (pageUrl) return of(pageUrl);

        return this.resolveEditor(contentlet.contentType).pipe(
            map(({ editor }) => buildContentletEditUrl(editor === 'new', contentlet.inode))
        );
    }
}

function buildPageEditUrl(contentlet: DotCMSContentlet): string | null {
    if (contentlet.baseType !== DotCMSBaseTypesContentTypes.HTMLPAGE) return null;
    const url = (contentlet['urlMap'] as string) || (contentlet['url'] as string);
    if (!url) return null;
    const params = new URLSearchParams({
        url,
        language_id: String(contentlet.languageId ?? 1),
        mId: 'edit'
    });
    return `/dotAdmin/#/edit-page/content?${params.toString()}`;
}

function buildContentletEditUrl(useNewEditor: boolean, inode: string): string {
    return useNewEditor ? `/dotAdmin/#/content/${inode}` : `/dotAdmin/#/c/content/${inode}`;
}
