/**
 * A tool group — a `Layout` in backend terms: the named bundle of portlets
 * that shows up as a section in the admin navigation.
 *
 * Served by `GET /v1/roles/layouts` (the system-wide catalog) and
 * `GET /v1/roles/{roleId}/layouts` (the groups granted to one role). Only the
 * catalog fills `portletTitles`; the per-role endpoint serializes raw `Layout`
 * objects.
 */
export interface DotToolGroup {
    readonly id: string;
    readonly name: string;
    readonly description?: string;
    /** Position in the navigation. Lower sorts first. */
    readonly tabOrder?: number;
    readonly portletIds?: string[];
    /**
     * Localized, human-readable names for `portletIds`, resolved server-side
     * against the `com.dotcms.repackage.javax.portlet.title.*` keys. Catalog
     * responses only — absent on the per-role endpoint.
     */
    readonly portletTitles?: string[];
}
