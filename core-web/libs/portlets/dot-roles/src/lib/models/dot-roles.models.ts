import { DotRole, DotToolGroup } from '@dotcms/dotcms-models';

// `DotRoleFormValue` moved to `@dotcms/dotcms-models` when create/update moved
// to the shared service. Re-exported so this portlet's call sites keep a
// single import for role types.
export type { DotRoleFormValue } from '@dotcms/dotcms-models';

/**
 * A node of the roles tree. Alias of the shared {@link DotRole} — the wire
 * shape is identical and there is one role model for the workspace.
 *
 * The names are kept because they carry the tree's intent at each call site
 * (a node in the hierarchy vs. the detail of the selected role) and because
 * the store, tree utils and components read better with them.
 *
 * Reminder for consumers: the backend hydrates only 2 levels per request, so
 * `roleChildren: []` is not evidence of a leaf — the store lazy-loads deeper
 * levels on `onNodeExpand`, and leaf status comes from `childCount`.
 */
export type DotRoleNode = DotRole;

/** Detail of the selected role. Same shape — see {@link DotRoleNode}. */
export type DotRoleDetail = DotRole;

/**
 * User row rendered in the Users tab table.
 *
 * `grantedFromRoleId` / `grantedFromRoleName` identify the ancestor role
 * where the user was directly granted. When it matches the currently-
 * selected role, the row is a direct grant and can be removed from this
 * tab; when it matches an ancestor, the row is inherited and can only
 * be removed by editing the ancestor role. The store walks the ancestor
 * chain via `RoleAPI.findRoleHierarchy` semantics to populate this.
 */
export interface DotRoleMember {
    readonly userId: string;
    readonly firstName: string;
    readonly lastName: string;
    readonly emailAddress: string;
    readonly grantedFromRoleId: string;
    readonly grantedFromRoleName: string;
}

/**
 * A tool group row in the Tools tab: the catalog entry plus where (if
 * anywhere) the selected role gets it from.
 *
 * `grantedFrom*` follows the same rule as `DotRoleMember`: when it matches the
 * selected role the grant is direct and can be toggled here; when it matches
 * an ancestor the grant is inherited and can only be revoked on that ancestor,
 * so the checkbox renders checked but disabled.
 */
export interface DotRoleToolGroupRow extends DotToolGroup {
    readonly granted: boolean;
    readonly grantedFromRoleId: string | null;
    readonly grantedFromRoleName: string | null;
}

export type DotRoleTab = 'users' | 'permissions' | 'tools';

// Re-export the shared component-status union so this portlet's status
// fields stay compatible with the rest of the codebase (CLAUDE.md rule).
export type { ComponentStatus as DotRolesStatus } from '@dotcms/dotcms-models';
