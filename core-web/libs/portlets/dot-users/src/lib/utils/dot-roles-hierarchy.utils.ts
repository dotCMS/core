import { forkJoin, Observable, of } from 'rxjs';

import { switchMap } from 'rxjs/operators';

import { DotRole } from '@dotcms/dotcms-models';

/**
 * Eager full-hierarchy loading strategy for the Roles tab.
 *
 * Lives here rather than in `DotRolesService` because it is not an endpoint —
 * it is one particular way of composing two endpoints, chosen for one
 * particular UI. The Roles tab renders a shuttle, so it needs every role up
 * front and cannot lazy-load; the roles portlet renders a tree and composes
 * the same endpoints lazily per expansion instead. Neither strategy belongs
 * to the shared service.
 *
 * `GET /v1/roles/_search` would be a single call, but it returns
 * `SmallRoleView` with no `parent`, so the hierarchy cannot be reconstructed
 * from it. Hence the walk.
 */

/**
 * Flatten the full role hierarchy into a list with `parent` set, so callers
 * can rebuild the tree with a plain parent-id lookup.
 *
 * @param roots roots with their first level hydrated
 *        (`getRoots(true)` — the backend fills only two levels per request)
 * @param fetchChildren resolves one role **with its children hydrated**;
 *        pass `(id) => rolesService.getById(id, true)`
 *
 * Errors propagate. A partial hierarchy rendered as if it were complete is
 * worse than a visible failure — the caller decides how to surface it.
 */
export function flattenRoleHierarchy(
    roots: DotRole[],
    fetchChildren: (roleId: string) => Observable<DotRole>
): Observable<DotRole[]> {
    const flat: DotRole[] = [];
    const seen = new Set<string>();

    const collect = (role: DotRole, parentId?: string) => {
        if (seen.has(role.id)) {
            return;
        }
        seen.add(role.id);
        flat.push({ ...role, roleChildren: undefined, parent: parentId });
    };

    /**
     * The backend marks a root by pointing `parent` at the role's own id.
     * Left as-is that makes a root look like its own child and breaks a
     * parent-id lookup, so it is normalized to `undefined`. Any other
     * `parent` is genuine and kept — the top level handed to this function
     * is not necessarily the true root of the hierarchy.
     */
    const normalizeParent = (role: DotRole) =>
        role.parent && role.parent !== role.id ? role.parent : undefined;

    for (const root of roots) {
        collect(root, normalizeParent(root));
        for (const child of root.roleChildren ?? []) {
            collect(child, root.id);
        }
    }

    return expandDescendants(
        flat.filter((role) => role.parent),
        flat,
        seen,
        fetchChildren
    );
}

/**
 * Breadth-first rounds until nothing new is discovered.
 *
 * Skips only the nodes whose `childCount` is confirmed zero — the filter is
 * `!== 0`, so an ABSENT `childCount` is still visited and a serializer that
 * omits the field degrades to the old exhaustive behavior rather than
 * silently truncating the tree. Most roles below the first level are leaves,
 * so this prunes the large majority of the request burst this walk used to
 * produce when the Roles tab opened.
 */
function expandDescendants(
    toExpand: DotRole[],
    flat: DotRole[],
    seen: Set<string>,
    fetchChildren: (roleId: string) => Observable<DotRole>
): Observable<DotRole[]> {
    const pending = toExpand.filter((role) => role.childCount !== 0);
    if (pending.length === 0) {
        return of(flat);
    }

    return forkJoin(
        pending.map((role) =>
            fetchChildren(role.id).pipe(
                switchMap((loaded) =>
                    of({ parentId: role.id, children: loaded.roleChildren ?? [] })
                )
            )
        )
    ).pipe(
        switchMap((results) => {
            const discovered: DotRole[] = [];
            for (const { parentId, children } of results) {
                for (const child of children) {
                    if (seen.has(child.id)) {
                        continue;
                    }
                    seen.add(child.id);
                    const node = { ...child, roleChildren: undefined, parent: parentId };
                    flat.push(node);
                    discovered.push(node);
                }
            }

            return expandDescendants(discovered, flat, seen, fetchChildren);
        })
    );
}
