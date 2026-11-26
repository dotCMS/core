# Contract: Batch Permission API

## Internal API Contracts

### 1. `PermissionFactory` / `PermissionBitFactoryImpl`

#### Method: `getBatchPermissions`
```java
/**
 * Queries the database in batches to determine which IDs are permitted and which IDs are
 * uncached (having neither an individual permission nor an entry in permission_reference).
 *
 * @param inodeIds   collection of permissionable IDs to check
 * @param permission required permission bit mask (e.g. PermissionAPI.PERMISSION_READ)
 * @param roleIds    already-resolved role IDs for the user
 * @return a BatchPermissionsResult containing permitted IDs and uncached IDs
 * @throws DotDataException on database access error
 */
BatchPermissionsResult getBatchPermissions(Collection<String> inodeIds, int permission, List<String> roleIds) throws DotDataException;
```

#### Method: `getPermittedIds` (Preserved for backward-compatibility)
```java
/**
 * Resolves permitted IDs using getBatchPermissions(inodeIds, permission, roleIds).getPermittedIds().
 */
@Override
Set<String> getPermittedIds(Collection<String> inodeIds, int permission, List<String> roleIds) throws DotDataException;
```

### 2. `PermissionAPI` / `PermissionBitAPIImpl`

#### Method: `filterCollection`
```java
/**
 * Filters a collection of Permissionables by the specified permission type for a given user.
 * 
 * Guarantees:
 * 1. CMS Administrator and system user bypass the check and receive the full input collection.
 * 2. Cached inheriting items and items with individual permissions are resolved via single batch query.
 * 3. Uncached inheriting items fall back to one-at-a-time parent walk-up and upsert to permission_reference.
 *    A PermissionableProxy is resolved to the real asset first (AssetPermissionHelper.resolveAsset);
 *    one that cannot be resolved stays denied, as before.
 * 4. The permission decision for an uncached item follows the batch filter rules, ensuring identical
 *    results before and after the item is cached in permission_reference.
 * 
 * @param permissionables    collection of items to filter
 * @param permissionType     bit mask of required permission
 * @param userIn             user requesting access (null treated as anonymous)
 * @param respectFrontendRoles whether anonymous and logged-in-site roles are included
 * @return list of items for which the user holds the required permission
 */
@Override
public <P extends Permissionable> List<P> filterCollection(
        final Collection<P> permissionables,
        final int permissionType,
        final User userIn,
        final boolean respectFrontendRoles) throws DotDataException, DotSecurityException;
```
