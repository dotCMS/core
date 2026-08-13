/**
 * Tree node used in the roles panel. Parent roles render with a folder icon
 * (they have children); leaf roles render with a shield icon.
 */
export interface DotRoleNode {
    readonly id: string;
    readonly name: string;
    readonly roleKey?: string;
    readonly parent?: string;
    readonly system?: boolean;
    readonly locked?: boolean;
    readonly userCount?: number;
    readonly children?: DotRoleNode[];
}

/**
 * Full role detail. Extends the tree node with the fields shown / edited
 * in the Add / Edit role dialogs.
 */
export interface DotRoleDetail extends DotRoleNode {
    readonly description?: string;
    readonly editUsers?: boolean;
    readonly editPermissions?: boolean;
    readonly editLayouts?: boolean;
    readonly DBFQN?: string;
    readonly FQN?: string;
}

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

/** Member of a role, annotated with the role that granted access. */
export interface DotRoleMember {
    readonly userId: string;
    readonly firstName: string;
    readonly lastName: string;
    readonly emailAddress: string;
    /** roleId that granted membership (self = direct; ancestor id = inherited) */
    readonly grantedFromRoleId: string;
    readonly grantedFromRoleName: string;
}

export type DotRoleTab = 'users' | 'permissions' | 'tools';

export type DotRolesStatus = 'init' | 'loading' | 'loaded' | 'error';
