import { DotRoleNode } from '../../models/dot-roles.models';

/**
 * Immutable helpers over the `DotRoleNode[]` tree used by `DotRolesStore`.
 * Kept pure so they can be unit-tested without spinning up the signal store,
 * and so the store file stays focused on state orchestration.
 */

/**
 * Upper bound on ancestor-chain length. Matches the semantic cap that the
 * Java `RoleAPI.findRoleHierarchy` code path assumes; if the BE ever bumps
 * this it must be raised here too. Also bounds the descent inside
 * `collectAncestorChain` against malformed data.
 */
export const MAX_ROLE_DEPTH = 20;

/**
 * Immutably splice `newChildren` into the tree under the node with `id`.
 * Returns a new tree; unchanged branches are shared by reference.
 */
export function patchNodeChildren(
    nodes: DotRoleNode[],
    id: string,
    newChildren: DotRoleNode[]
): DotRoleNode[] {
    return nodes.map((node) => {
        if (node.id === id) {
            return { ...node, roleChildren: newChildren };
        }
        if (node.roleChildren && node.roleChildren.length > 0) {
            return {
                ...node,
                roleChildren: patchNodeChildren(node.roleChildren, id, newChildren)
            };
        }
        return node;
    });
}

/**
 * Immutably replace the node with `id`, preserving `roleChildren` from the
 * previous version. Used by updateRole when the parent hasn't changed — the
 * server response may omit deeper descendants we already lazy-loaded.
 */
export function patchNodeInPlace(
    nodes: DotRoleNode[],
    id: string,
    replacement: DotRoleNode
): DotRoleNode[] {
    return nodes.map((node) => {
        if (node.id === id) {
            return { ...replacement, roleChildren: node.roleChildren ?? [] };
        }
        if (node.roleChildren && node.roleChildren.length > 0) {
            return {
                ...node,
                roleChildren: patchNodeInPlace(node.roleChildren, id, replacement)
            };
        }
        return node;
    });
}

/**
 * Immutably drop the node with `id` from anywhere in the tree.
 *
 * Decrements the parent's `childCount` when the removal actually took a child
 * from it — see {@link appendChildToParent} for why that field cannot be left
 * stale. Only adjusted when a child was really removed at this level, so an
 * untouched branch keeps its count.
 */
export function removeNodeFromTree(nodes: DotRoleNode[], id: string): DotRoleNode[] {
    return nodes.reduce<DotRoleNode[]>((acc, node) => {
        if (node.id === id) {
            return acc;
        }
        if (node.roleChildren && node.roleChildren.length > 0) {
            const roleChildren = removeNodeFromTree(node.roleChildren, id);
            const removedHere = roleChildren.length < node.roleChildren.length;

            acc.push({
                ...node,
                childCount: removedHere
                    ? Math.max(0, (node.childCount ?? node.roleChildren.length) - 1)
                    : node.childCount,
                roleChildren
            });
        } else {
            acc.push(node);
        }
        return acc;
    }, []);
}

/**
 * Immutably append a newly-created role to its parent's `roleChildren`.
 * Sharing branches by reference keeps re-render churn minimal.
 *
 * `childCount` is bumped alongside the splice. The tree treats that field as
 * authoritative for leaf-vs-chevron, so leaving it stale at 0 would render a
 * parent that just gained its first child as a childless leaf — with no
 * chevron, the new role is unreachable until a full reload.
 */
export function appendChildToParent(
    nodes: DotRoleNode[],
    parentId: string,
    child: DotRoleNode
): DotRoleNode[] {
    return nodes.map((node) => {
        if (node.id === parentId) {
            const roleChildren = [...(node.roleChildren ?? []), child];

            return {
                ...node,
                childCount: Math.max(node.childCount ?? 0, roleChildren.length),
                roleChildren
            };
        }
        if (node.roleChildren && node.roleChildren.length > 0) {
            return {
                ...node,
                roleChildren: appendChildToParent(node.roleChildren, parentId, child)
            };
        }
        return node;
    });
}

/**
 * Immutably set a node's `userCount`, leaving everything else untouched.
 *
 * The tree badge reads this field, but it only arrives with the role payload —
 * granting or revoking a user does not refresh it, so it goes stale the moment
 * the admin edits membership. Callers sync it from the direct-grant count they
 * just computed.
 */
export function patchNodeUserCount(
    nodes: DotRoleNode[],
    id: string,
    userCount: number
): DotRoleNode[] {
    return nodes.map((node) => {
        if (node.id === id) {
            return { ...node, userCount };
        }
        if (node.roleChildren && node.roleChildren.length > 0) {
            return {
                ...node,
                roleChildren: patchNodeUserCount(node.roleChildren, id, userCount)
            };
        }

        return node;
    });
}

/**
 * Build the ancestor chain for a role, ordered `[role, parent, grandparent,
 * ..., root]`. The chain drives the parallel per-ancestor fan-out in
 * `loadMembers` and `loadToolGroups`, so a chain that is short by one role
 * silently under-reports inherited grants.
 *
 * Walks the tree STRUCTURE rather than following `parent` ids. Both trees this
 * runs against nest a role beneath its ancestors, but only one of them
 * populates `parent`: `unwrapLegacySearchNode` keeps id/name/locked and drops
 * it, so a role reached through the search results produced a one-element
 * chain and lost every inherited member and tool group. Nesting is present in
 * both, which also makes the `parent === id` root convention moot here.
 *
 * Returns `[]` when the role isn't in the tree at all. That means "the chain
 * cannot be built", NOT "the role has no ancestors" — the two are
 * indistinguishable to a caller once a stub stands in for the missing node,
 * and the caller then renders a direct-grants-only answer as if it were
 * complete. Callers must surface the empty chain instead of fanning out.
 */
export function collectAncestorChain(tree: DotRoleNode[], role: { id: string }): DotRoleNode[] {
    const path = findPathToRole(tree, role.id);

    // `findPathToRole` returns root-first; the fan-out wants closest-first so
    // a direct grant wins over an inherited one.
    return path ? path.reverse() : [];
}

/**
 * Path from a root down to `id`, ordered `[root, ..., role]`, or `null` when
 * the id isn't in the tree. Depth-capped by {@link MAX_ROLE_DEPTH} so
 * malformed data (a branch spliced into its own subtree) cannot recurse
 * forever.
 */
function findPathToRole(nodes: DotRoleNode[], id: string, depth = 0): DotRoleNode[] | null {
    if (depth >= MAX_ROLE_DEPTH) {
        return null;
    }

    for (const node of nodes) {
        if (node.id === id) {
            return [node];
        }
        if (node.roleChildren && node.roleChildren.length > 0) {
            const below = findPathToRole(node.roleChildren, id, depth + 1);
            if (below) {
                return [node, ...below];
            }
        }
    }

    return null;
}

/**
 * Put two role trees side by side so {@link collectAncestorChain} can reach a
 * node in either. Under an active search the tree renders `searchResults`,
 * which carries branches the lazily-loaded `roles` cache has never fetched;
 * `roles` in turn holds branches the search never matched.
 *
 * Deliberately NOT deduped by id. The same root can appear in both with
 * different subtrees hydrated — the cache copy unexpanded, the search copy
 * carrying the matched descendant — and picking one would drop the path the
 * other holds. `collectAncestorChain` scans roots in order and stops at the
 * first that contains the target, so a duplicate root costs a miss and the
 * cache copy stays preferred when both can answer.
 */
export function mergeTreesForLookup(
    primary: DotRoleNode[],
    fallback: DotRoleNode[]
): DotRoleNode[] {
    return fallback.length === 0 ? primary : [...primary, ...fallback];
}

/**
 * Fold repeated ids out of a sibling list, keeping the first occurrence.
 *
 * `PUT /v1/roles/{roleId}` reports the updated role's children multiplied —
 * one child comes back four times, and `childCount` is computed from the same
 * list so it is inflated to match (dotCMS/core#37303). Only the response body
 * is affected; the persisted rows are correct, which is why a reload clears
 * it. Everything the store reads out of that response goes through here first.
 */
export function dedupeRolesById(nodes: DotRoleNode[]): DotRoleNode[] {
    const seen = new Set<string>();

    return nodes.filter((node) => {
        if (seen.has(node.id)) {
            return false;
        }
        seen.add(node.id);

        return true;
    });
}

/** Walk the tree looking for a node id. Returns the node or `null`. */
export function findRoleInTree(nodes: DotRoleNode[], id: string): DotRoleNode | null {
    for (const node of nodes) {
        if (node.id === id) {
            return node;
        }
        if (node.roleChildren && node.roleChildren.length > 0) {
            const found = findRoleInTree(node.roleChildren, id);
            if (found) {
                return found;
            }
        }
    }

    return null;
}
