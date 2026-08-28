import { DotRoleNode } from '../../models/dot-roles.models';

/**
 * Immutable helpers over the `DotRoleNode[]` tree used by `DotRolesStore`.
 * Kept pure so they can be unit-tested without spinning up the signal store,
 * and so the store file stays focused on state orchestration.
 */

/**
 * Upper bound on ancestor-chain length. Matches the semantic cap that the
 * Java `RoleAPI.findRoleHierarchy` code path assumes; if the BE ever bumps
 * this it must be raised here too. Also guards against pathological cycles
 * that would otherwise loop forever inside `collectAncestorChain`.
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
 * ..., root]`. Matches the Java `RoleAPI.findRoleHierarchy` semantics (a
 * self-referential `parent === id` marks the root). The chain drives the
 * parallel `GET /v1/roles/{roleId}/users` fan-out in `loadMembers`.
 */
export function collectAncestorChain(tree: DotRoleNode[], role: { id: string }): DotRoleNode[] {
    const start = findRoleInTree(tree, role.id) ?? { id: role.id, name: role.id };
    const chain: DotRoleNode[] = [start];
    let cursor: DotRoleNode | null = start;
    // Guard against pathological data — see `MAX_ROLE_DEPTH` doc.
    for (let i = 0; i < MAX_ROLE_DEPTH; i++) {
        const parentId = cursor.parent;
        if (!parentId || parentId === cursor.id) {
            break;
        }
        const parentNode = findRoleInTree(tree, parentId);
        if (!parentNode) {
            break;
        }
        chain.push(parentNode);
        cursor = parentNode;
    }

    return chain;
}

/**
 * Merge two role trees at the root level so lookups can reach any node in
 * either. When the same id appears in both, prefer the copy that carries a
 * `parent` — search-result nodes lose that field
 * (`unwrapLegacySearchNode` only keeps id/name/locked), and
 * `collectAncestorChain` needs `parent` to climb.
 *
 * Only dedupes at the roots; child branches are not merged deeply because
 * both trees are self-consistent hierarchies rooted at different depths.
 */
export function mergeTreesPreferParent(
    primary: DotRoleNode[],
    fallback: DotRoleNode[]
): DotRoleNode[] {
    if (fallback.length === 0) {
        return primary;
    }
    const seen = new Map<string, DotRoleNode>();
    for (const node of primary) {
        seen.set(node.id, node);
    }
    for (const node of fallback) {
        const prior = seen.get(node.id);
        if (!prior) {
            seen.set(node.id, node);
        } else if (!prior.parent && node.parent) {
            // Fallback carries a `parent`; primary doesn't — prefer the
            // fallback so the ancestor walk can actually climb.
            seen.set(node.id, node);
        }
    }

    return Array.from(seen.values());
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
