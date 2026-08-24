import { DotRoleNode } from '../../models/dot-roles.models';

/**
 * Immutable helpers over the `DotRoleNode[]` tree used by `DotRolesStore`.
 * Kept pure so they can be unit-tested without spinning up the signal store,
 * and so the store file stays focused on state orchestration.
 */

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

/** Immutably drop the node with `id` from anywhere in the tree. */
export function removeNodeFromTree(nodes: DotRoleNode[], id: string): DotRoleNode[] {
    return nodes.reduce<DotRoleNode[]>((acc, node) => {
        if (node.id === id) {
            return acc;
        }
        if (node.roleChildren && node.roleChildren.length > 0) {
            acc.push({
                ...node,
                roleChildren: removeNodeFromTree(node.roleChildren, id)
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
 */
export function appendChildToParent(
    nodes: DotRoleNode[],
    parentId: string,
    child: DotRoleNode
): DotRoleNode[] {
    return nodes.map((node) => {
        if (node.id === parentId) {
            return {
                ...node,
                roleChildren: [...(node.roleChildren ?? []), child]
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
 * Build the ancestor chain for a role, ordered `[role, parent, grandparent,
 * ..., root]`. Matches the Java `RoleAPI.findRoleHierarchy` semantics (a
 * self-referential `parent === id` marks the root). The chain drives the
 * parallel `/v1/users/filter?roleKey=X` fan-out in `loadMembers`.
 */
export function collectAncestorChain(
    tree: DotRoleNode[],
    role: { id: string; roleKey?: string | null }
): DotRoleNode[] {
    const start = findRoleInTree(tree, role.id) ?? {
        id: role.id,
        name: role.id,
        roleKey: role.roleKey ?? undefined
    };
    const chain: DotRoleNode[] = [start];
    let cursor: DotRoleNode | null = start;
    // Guard against pathological data — hierarchies deeper than 20 aren't
    // realistic in practice, and this stops any accidental cycles cold.
    for (let i = 0; i < 20; i++) {
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
