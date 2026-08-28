/**
 * Shape returned by `GET /v1/roles` and `GET /v1/roles/{roleid}` — mirrors
 * the backend `RoleView`. The wire response is **nested**: each node carries
 * its direct children under `roleChildren`. The backend loads only 2 levels
 * per request, so grandchildren come back as `roleChildren: []` even when
 * they exist. The store lazy-loads deeper levels on `onNodeExpand` in the
 * roles tree.
 *
 * `user: true` marks the role as an individual user-role (roleKey = userId).
 * User-roles are always leaves in the tree.
 */
export interface DotRoleNode {
    readonly id: string;
    readonly name: string;
    readonly roleKey?: string;
    readonly parent?: string;
    readonly system?: boolean;
    readonly locked?: boolean;
    readonly user?: boolean;
    readonly editUsers?: boolean;
    readonly editPermissions?: boolean;
    readonly editLayouts?: boolean;
    /** Direct children — populated by `/v1/roles?loadChildrenRoles=true`. */
    readonly roleChildren?: DotRoleNode[];
}

/** Full role detail. Same shape as `DotRoleNode` today. */
export type DotRoleDetail = DotRoleNode & {
    readonly description?: string;
    readonly DBFQN?: string;
    readonly FQN?: string;
};

/**
 * Payload for POST /v1/roles (RoleForm).
 * Also the shape the Angular Edit Role dialog collects; it PUTs the same
 * body against /v1/roles/{roleId} once the endpoint ships (see #36936).
 */
export interface DotRoleFormValue {
    roleName: string;
    roleKey?: string;
    parentRoleId?: string | null;
    canEditUsers: boolean;
    canEditPermissions: boolean;
    canEditLayouts: boolean;
    description?: string;
}

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

export type DotRoleTab = 'users' | 'permissions' | 'tools';

// Re-export the shared component-status union so this portlet's status
// fields stay compatible with the rest of the codebase (CLAUDE.md rule).
export type { ComponentStatus as DotRolesStatus } from '@dotcms/dotcms-models';
