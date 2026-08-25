import { DotRoleFormValue, DotRoleNode } from '../models/dot-roles.models';

/**
 * Pure request/response adapters used by `DotRolesPortletService`. Extracted
 * out of the service so the delicate mapping logic (legacy Dojo shape,
 * hierarchy-vs-user split, empty-string sanitization for the `role_key`
 * UNIQUE constraint) can be unit-tested without HTTP mocking.
 */

/**
 * User row shape returned by `/v1/users/filter?roleKey=X`. The endpoint
 * accepts `roleKey` (not `roleId`) as the filter parameter. Same wire
 * format used by the dot-users portlet.
 */
export interface DotRoleUserFilterResult {
    readonly userId: string;
    readonly firstName?: string;
    readonly lastName?: string;
    readonly emailAddress?: string;
}

/** Legacy `RoleResource.buildFilteredJsonTree` node shape. */
export interface LegacyRoleSearchNode {
    readonly id: string;
    readonly name: string;
    readonly locked?: boolean;
    readonly children?: LegacyRoleSearchNode[];
}

/** Row shape from `/v1/roles/{roleId}/rolehierarchyanduserroles`. */
export interface RoleHierarchyEntry {
    readonly id: string;
    readonly name?: string;
    readonly roleKey?: string;
    readonly user?: boolean;
}

/**
 * Matches a UUID-shape id with underscores where a real UUID would have
 * dashes — that's the exact wire form the legacy Dojo serializer produces
 * (`r.getId().replace('-', '_')` in `RoleResource.buildFilteredJsonTree`).
 * Restricting the reverse-replace to this shape avoids mangling user role
 * keys that legitimately contain underscores (e.g. `DOTCMS_BACK_END_USER`).
 */
const LEGACY_UUID_WITH_UNDERSCORES =
    /^[a-fA-F0-9]{8}_[a-fA-F0-9]{4}_[a-fA-F0-9]{4}_[a-fA-F0-9]{4}_[a-fA-F0-9]{12}$/;

/**
 * Adapt a `LegacyRoleSearchNode` into the modern `DotRoleNode` shape.
 * The legacy payload underscores dashes in the id — we only reverse the
 * substitution when the id actually matches the UUID-with-underscores
 * shape, so ids that legitimately contain underscores are preserved.
 */
export function unwrapLegacySearchNode(node: LegacyRoleSearchNode): DotRoleNode {
    return {
        id: LEGACY_UUID_WITH_UNDERSCORES.test(node.id) ? node.id.replace(/_/g, '-') : node.id,
        name: node.name,
        locked: node.locked,
        roleChildren: (node.children ?? []).map(unwrapLegacySearchNode)
    };
}

/**
 * Filter and adapt the mixed Role list from
 * `/v1/roles/{roleId}/rolehierarchyanduserroles` down to user rows.
 * The endpoint returns Role objects for both real roles and users (users
 * are Role rows with `user === true` and `roleKey === userId`), and never
 * carries email — so the `emailAddress` is always empty here.
 */
export function toRoleMemberResults(
    entries: readonly RoleHierarchyEntry[]
): DotRoleUserFilterResult[] {
    return entries
        .filter((entry) => entry.user === true)
        .map((entry) => {
            const [firstName = '', ...rest] = (entry.name ?? '').split(' ');

            return {
                userId: entry.roleKey ?? entry.id,
                firstName,
                lastName: rest.join(' '),
                emailAddress: ''
            };
        });
}

/**
 * Trim optional string fields and drop them when empty so JSON.stringify
 * omits them from the request body — the `cms_role.role_key` column has a
 * UNIQUE constraint at the DB level and legacy roles ship with
 * `role_key = ''`, so posting `""` reliably trips a duplicate-key violation
 * from Postgres. Sending `undefined` lets the BE persist NULL and satisfy
 * the uniqueness contract.
 */
export function sanitizeRoleForm(form: DotRoleFormValue): DotRoleFormValue {
    const trimmedKey = form.roleKey?.trim();
    const trimmedDescription = form.description?.trim();

    return {
        ...form,
        roleKey: trimmedKey ? trimmedKey : undefined,
        description: trimmedDescription ? trimmedDescription : undefined,
        parentRoleId: form.parentRoleId ?? undefined
    };
}
