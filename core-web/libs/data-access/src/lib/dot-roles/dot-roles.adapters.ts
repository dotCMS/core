import { DotRole, DotRoleFormValue } from '@dotcms/dotcms-models';

/**
 * Pure request/response adapters for `DotRolesService`. Kept out of the
 * service so the delicate mapping logic (the legacy Dojo tree shape, the
 * empty-string sanitization the `role_key` UNIQUE constraint forces) can be
 * unit-tested without HTTP mocking.
 */

/** Legacy `RoleResource.buildFilteredJsonTree` node shape. */
export interface LegacyRoleSearchNode {
    readonly id: string;
    readonly name: string;
    readonly locked?: boolean;
    readonly children?: LegacyRoleSearchNode[];
}

/** Wire envelope of the legacy Dojo `ItemFileReadStore` search response. */
export interface LegacyRoleSearchResponse {
    readonly identifier?: string;
    readonly label?: string;
    readonly items?: Array<{
        readonly id?: string;
        readonly name?: string;
        readonly top?: boolean;
        readonly children?: LegacyRoleSearchNode[];
    }>;
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
 * Adapt a `LegacyRoleSearchNode` into the modern `DotRole` shape. The legacy
 * payload underscores dashes in the id — we only reverse the substitution when
 * the id actually matches the UUID-with-underscores shape, so ids that
 * legitimately contain underscores are preserved.
 */
export function unwrapLegacySearchNode(node: LegacyRoleSearchNode): DotRole {
    return {
        id: LEGACY_UUID_WITH_UNDERSCORES.test(node.id) ? node.id.replace(/_/g, '-') : node.id,
        name: node.name,
        locked: node.locked,
        roleChildren: (node.children ?? []).map(unwrapLegacySearchNode)
    };
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
