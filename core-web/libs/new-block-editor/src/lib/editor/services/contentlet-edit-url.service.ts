import { Observable, of } from 'rxjs';

import { Injectable, inject } from '@angular/core';

import { catchError, map } from 'rxjs/operators';

import { DotContentTypeService } from '@dotcms/data-access';
import { FeaturedFlags } from '@dotcms/dotcms-models';

interface ResolveInput {
    inode: string;
    contentType: string;
}

/**
 * Resolves the destination URL for editing an embedded `dotContent` contentlet, choosing
 * between the legacy and new dotCMS content editors based on the contentlet's content
 * type metadata (`FEATURE_FLAG_CONTENT_EDITOR2_ENABLED`). Each content type's flag is
 * looked up once per service lifetime and cached, so repeated edits of contentlets of
 * the same type don't re-hit the network.
 *
 * The legacy bubble-menu (`libs/block-editor`) fetched the flag per click — we preserve
 * that semantic exactly, just with caching to avoid the duplicate fetches a customer
 * would notice when editing multiple contentlets of the same type in one session.
 */
@Injectable()
export class ContentletEditUrlService {
    private readonly contentTypeService = inject(DotContentTypeService);

    /** Cache: content type variable → useNewEditor (true ⇒ /dotAdmin/#/content, false ⇒ /dotAdmin/#/c/content). */
    private readonly cache = new Map<string, boolean>();

    /**
     * Resolves the absolute URL to navigate to when the user wants to edit `input`'s
     * contentlet. Cached per content type variable; falls back to the legacy URL on
     * any network / metadata read error so we never break the user's flow on a 4xx.
     */
    resolveEditUrl(input: ResolveInput): Observable<string> {
        const cached = this.cache.get(input.contentType);
        if (cached !== undefined) {
            return of(buildEditUrl(cached, input.inode));
        }

        return this.contentTypeService.getContentType(input.contentType).pipe(
            catchError(() => of(null)),
            map((ct) => {
                const useNew = !!ct?.metadata?.[FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED];
                this.cache.set(input.contentType, useNew);
                return buildEditUrl(useNew, input.inode);
            })
        );
    }
}

function buildEditUrl(useNewEditor: boolean, inode: string): string {
    return useNewEditor ? `/dotAdmin/#/content/${inode}` : `/dotAdmin/#/c/content/${inode}`;
}
