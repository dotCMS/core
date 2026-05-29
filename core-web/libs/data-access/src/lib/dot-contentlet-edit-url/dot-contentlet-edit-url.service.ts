import { Observable, of } from 'rxjs';

import { Injectable, inject } from '@angular/core';

import { catchError, map } from 'rxjs/operators';

import {
    DotCMSBaseTypesContentTypes,
    DotCMSContentlet,
    FeaturedFlags
} from '@dotcms/dotcms-models';

import { DotContentTypeService } from '../dot-content-type/dot-content-type.service';

/**
 * Canonical resolver for "where in dotAdmin should I open this contentlet for editing?".
 *
 * This service exists to consolidate logic that has been re-implemented inline at multiple
 * call sites across the admin (Query Tool, Content Drive's `#editContentlet`, the legacy and
 * new block-editor "edit contentlet" actions). Adopting this service means future changes —
 * editor URL format updates, feature-flag renames, new fallback rules — land in one place
 * instead of drifting across copies.
 *
 * ## What it resolves
 *
 * Given a contentlet, returns the dotAdmin hash route that should open it for editing.
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
 * The content-type → `useNewEditor` boolean is cached for the lifetime of the application
 * (the service is `providedIn: 'root'`). A list view rendering 50 contentlets of the same
 * content type hits `/api/v1/contenttype/id/{type}` once, not 50 times.
 *
 * **Cache invalidation caveat:** if an admin toggles `CONTENT_EDITOR2_ENABLED` on a content
 * type during the same session, cached entries go stale until a full page reload. This
 * matches the behavior of the prior inline implementations and is acceptable because the
 * flag is rarely toggled at runtime.
 *
 * ## Error fallback
 *
 * If the content-type lookup fails (4xx/5xx, network), the resolver returns the **legacy**
 * editor URL rather than propagating the error. Rationale: the legacy editor handles every
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

    // Cached per content-type variable: true ⇒ new editor, false ⇒ legacy editor.
    readonly #editorFlagCache = new Map<string, boolean>();

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

        const cached = this.#editorFlagCache.get(contentlet.contentType);
        if (cached !== undefined) {
            return of(buildContentletEditUrl(cached, contentlet.inode));
        }

        return this.#contentTypeService.getContentType(contentlet.contentType).pipe(
            catchError(() => of(null)),
            map((ct) => {
                const useNewEditor =
                    !!ct?.metadata?.[FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED];
                this.#editorFlagCache.set(contentlet.contentType, useNewEditor);
                return buildContentletEditUrl(useNewEditor, contentlet.inode);
            })
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
