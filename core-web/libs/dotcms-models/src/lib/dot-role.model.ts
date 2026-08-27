/**
 * The one role shape for the workspace. Mirrors the backend `RoleView`
 * (`com.dotcms.rest.api.v1.system.role.RoleView`) plus the two fields the
 * legacy `Role` payloads add.
 *
 * Only `id` and `name` are guaranteed: the `/api/v1/roles/**` surface is
 * served by three different serializers and each one omits part of the
 * shape.
 *
 * | Source | What it fills |
 * |---|---|
 * | `GET /v1/roles`, `GET /v1/roles/{id}`, `GET /v1/roles/users/{id}` | full `RoleView` — everything except `user` |
 * | `GET /v1/roles/_search` | `SmallRoleView` — no `parent`, no hierarchy |
 * | `GET /v1/roles/{id}/rolehierarchyanduserroles` | legacy `Role` — carries `user`, no counts |
 *
 * Consumers must therefore treat every optional field as genuinely absent
 * rather than defaulting it — in particular `childCount === undefined` means
 * "unknown", not zero.
 */
export interface DotRole {
    id: string;
    name: string;
    /**
     * True when the role is an individual user-role (`roleKey` holds the
     * user id). Only present on the legacy `Role` payloads — `RoleView`
     * does not serialize it. User-roles are always leaves.
     */
    user?: boolean;
    /**
     * Stable lookup key. Absent on roles created through the UI without an
     * explicit key, so it can never be assumed as an identifier — use `id`.
     */
    roleKey?: string;
    description?: string;
    /** Parent role id. A self-referential `parent === id` marks a root. */
    parent?: string;
    system?: boolean;
    locked?: boolean;
    editUsers?: boolean;
    editPermissions?: boolean;
    editLayouts?: boolean;
    /**
     * Fully-qualified names. Serialized by Jackson in all-caps because the
     * Java getters are `getDBFQN()` / `getFQN()` — the lowercase spelling
     * never matched the wire.
     */
    DBFQN?: string;
    FQN?: string;
    /**
     * Direct children, populated by `loadChildrenRoles=true`. The backend
     * hydrates only two levels per request, so grandchildren come back as an
     * empty array even when they exist — an empty `roleChildren` is not
     * evidence of a leaf. Use `childCount` for that.
     */
    roleChildren?: DotRole[];
    /**
     * Number of direct child roles, independent of whether `roleChildren`
     * was hydrated. Lets a tree decide chevron-vs-leaf, and lets an eager
     * walk skip fetching known leaves.
     */
    childCount?: number;
    /**
     * Number of users granted this role **directly**. Excludes inherited
     * grants and users hidden from the listing (system, anonymous, default,
     * flagged for deletion), so it matches the total of
     * `GET /v1/roles/{roleId}/users`.
     */
    userCount?: number;
}

/**
 * Request body for `POST /v1/roles` and `PUT /v1/roles/{roleId}` (the backend
 * `RoleForm`). PUT is a full replace, so an omitted field is cleared, not
 * preserved — callers editing a role must send every field they want to keep.
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
