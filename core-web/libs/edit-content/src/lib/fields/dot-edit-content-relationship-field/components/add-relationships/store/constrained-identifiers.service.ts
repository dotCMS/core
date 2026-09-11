import { Observable, of } from 'rxjs';

import { inject, Injectable } from '@angular/core';

import { catchError, map } from 'rxjs/operators';

import { DotContentSearchService } from '@dotcms/data-access';
import { DotCMSContentlet } from '@dotcms/dotcms-models';

/**
 * Upper bound on the parents scanned in one query.
 *
 * The check needs every parent of the type, because any one of them may hold the child the editor
 * is about to take. A cap is still needed so a pathological content type cannot hang the dialog.
 */
const CONSTRAINED_QUERY_LIMIT = 5000;

/**
 * Finds content that is already claimed by a **different** parent.
 *
 * Only meaningful when the field is the parent side of a **ONE_TO_ONE** or **ONE_TO_MANY**
 * relationship — the two cardinalities where a child may belong to exactly one parent. In every
 * other shape a child can be related from many places at once and nothing is constrained.
 *
 * Why it matters: without it the editor can relate a child that already has a parent, and saving
 * **takes it from that parent silently**. The row is shown in the list either way — it exists and
 * hiding it would be confusing — but it must not be selectable.
 *
 * Carried over from `ExistingContentService` when that dialog was replaced, unchanged apart from
 * living on its own: it is one query with one job, and folding it into the picker's store would
 * have mixed a relationship-integrity rule into a browse store that knows nothing about parents.
 */
@Injectable({ providedIn: 'root' })
export class ConstrainedIdentifiersService {
    readonly #contentSearchService = inject(DotContentSearchService);

    /**
     * Identifiers already related to some other parent through this relationship.
     *
     * @param params.parentContentTypeId Inode of the parent content type.
     * @param params.fieldVariable The relationship field's variable, e.g. `relation`.
     * @param params.currentContentIdentifier The contentlet being edited, so its **own** children
     *   are not reported as taken. Null for a contentlet that has never been saved.
     * @return The taken identifiers. Empty on failure — a lookup that could not run must not make
     *   every row unselectable.
     */
    get(params: {
        parentContentTypeId: string;
        fieldVariable: string;
        currentContentIdentifier: string | null;
    }): Observable<Set<string>> {
        const { parentContentTypeId, fieldVariable } = params;

        if (!parentContentTypeId || !fieldVariable) {
            return of(new Set<string>());
        }

        return this.#contentSearchService
            .get<{ jsonObjectView: { contentlets: DotCMSContentlet[] } }>({
                query: `+structureInode:${parentContentTypeId} +working:true +deleted:false`,
                sort: 'modDate desc',
                limit: CONSTRAINED_QUERY_LIMIT,
                offset: 0,
                depth: 0
            })
            .pipe(
                map(({ jsonObjectView: { contentlets } }) => {
                    const constrainedIds = new Set<string>();

                    for (const parent of contentlets) {
                        if (parent.identifier === params.currentContentIdentifier) {
                            continue;
                        }

                        const relatedChildren = parent[fieldVariable] as unknown;

                        // ONE_TO_ONE returns a single value; ONE_TO_MANY returns an array.
                        const children = Array.isArray(relatedChildren)
                            ? relatedChildren
                            : relatedChildren != null
                              ? [relatedChildren]
                              : [];

                        for (const child of children as unknown[]) {
                            const childId =
                                typeof child === 'string'
                                    ? child
                                    : child != null &&
                                        typeof child === 'object' &&
                                        'identifier' in child
                                      ? (child as { identifier: string }).identifier
                                      : null;

                            if (childId) {
                                constrainedIds.add(childId);
                            }
                        }
                    }

                    return constrainedIds;
                }),
                catchError(() => of(new Set<string>()))
            );
    }
}
