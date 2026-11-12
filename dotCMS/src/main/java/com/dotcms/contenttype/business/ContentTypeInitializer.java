package com.dotcms.contenttype.business;

import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.config.DotInitializer;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.ImmutableBinaryField;
import com.dotcms.contenttype.model.field.ImmutableTextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ImmutableSimpleContentType;
import com.dotcms.contenttype.model.type.SimpleContentType;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.LocalTransaction;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import io.vavr.Lazy;
import io.vavr.control.Try;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Initialiaze content types
 * @author jsanca
 */
public class ContentTypeInitializer implements DotInitializer {

    public static final String LEGACY_FAVORITE_PAGE_VAR_NAME = "favoritePage";
    public static final String FAVORITE_PAGE_VAR_NAME = "dotFavoritePage";

    /** System table key written once a repair has reset the favorites' own permission rows. */
    public static final String FAVORITE_PAGE_PERMISSIONS_RESET_KEY = "FAVORITE_PAGE_PERMISSIONS_RESET";

    private static final String FAVORITE_PAGE_PERMISSIONS_LOCK = "favorite-page-permissions";
    private static final String REPAIR_LOCK_SQL = "SELECT pg_advisory_xact_lock(hashtext(?))";
    private static final String COUNT_RESET_MARKER_SQL = "select count(*) as total from system_table where key = ?";
    private static final String COUNT_FAVORITES_WITH_OWN_ROWS_SQL = "select count(distinct inode_id) as total from permission where inode_id in (select identifier from contentlet where structure_inode = ?)";
    private static final String FAVORITES_OFF_SYSTEM_HOST_SQL = "select distinct i.id from identifier i join contentlet c on c.identifier = i.id where c.structure_inode = ? and (i.host_inode <> ? or i.parent_path <> ?)";
    /** Seconds after the repair commits before the permission cache is cleared a second time. */
    private static final long PERMISSION_CACHE_RECLEAR_SECONDS = 30;

    private static final Lazy<Boolean> doDefaultPagePermissions = Lazy.of(()->Config.getBooleanProperty("DO_DEFAULT_PAGE_PERMISSIONS", true));

    @Override
    public void init() {

        this.checkFavoritePage();
    }

    private void checkFavoritePage() {

        final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());

        //I'll try to remove the existing content type using the legacy var name (favoritePage), as it was renamed to dotFavoritePage
        //By the time it was created as `favoritePage`, this feature had not been released, so I don't need to worry about existing pieces of content
        ContentType contentType = Try.of(()->contentTypeAPI.find(LEGACY_FAVORITE_PAGE_VAR_NAME)).getOrNull();
        if (null != contentType){
            try {
                contentTypeAPI.delete(contentType);
            } catch (DotSecurityException | DotDataException e) {
                Logger.warnAndDebug(this.getClass(), e);
            }
        }

        contentType = Try.of(()->contentTypeAPI.find(FAVORITE_PAGE_VAR_NAME)).getOrNull();
        if (null == contentType) {
            Logger.info(this, "Creating the Favorite Page Content Type...");
            final ImmutableSimpleContentType.Builder builder = ImmutableSimpleContentType.builder();
            builder.name("Dot Favorite Page")
                    .variable(FAVORITE_PAGE_VAR_NAME)
                    .host(Host.SYSTEM_HOST)
                    .host(Folder.SYSTEM_FOLDER)
                    .system(true)
                    .fixed(true);
            final SimpleContentType simpleContentType = builder.build();

            saveFavoritePageFields(contentTypeAPI, simpleContentType);
        } else {
            // if the content type exists, we need to see if latest changes are there, otherwise we need to redefine the content type.
            if (contentType.fieldMap().get("url").unique() || !contentType.fieldMap().get("order").indexed()) {
                Logger.debug(ContentTypeInitializer.class, "dotFavoritePage CT Needs to be regenerated.");
                if (!contentType.fixed()) { // if the content type is not unchangeable
                    this.saveFavoritePageFields(contentTypeAPI, contentType);
                }
            }
        }

        contentType = null == contentType?Try.of(
                ()->contentTypeAPI.find(FAVORITE_PAGE_VAR_NAME)).getOrNull(): contentType;

        if (null != contentType) {
            checkDefaultPermissions(contentType);
            checkIfMarkedAsSystem(contentType, contentTypeAPI);
        }
    }

    private void checkIfMarkedAsSystem(final ContentType contentType, final ContentTypeAPI contentTypeAPI) {

        if (!contentType.system()) {

            try {
                final ImmutableSimpleContentType newType = ImmutableSimpleContentType.builder()
                        .from(contentType).system(true).build();

                final ContentType savedContentType = contentTypeAPI.save(newType);
                APILocator.getWorkflowAPI().saveSchemeIdsForContentType(savedContentType,
                        new HashSet<>(Arrays.asList(WorkflowAPI.SYSTEM_WORKFLOW_ID)));
            } catch (DotDataException | DotSecurityException e) {

                Logger.warnAndDebug(this.getClass(), e);
            }
        }
    }

    private void saveFavoritePageFields(final ContentTypeAPI contentTypeAPI, final ContentType simpleContentType) {
        try {

            final List<Field> newFields = new ArrayList<>();
            final ImmutableBinaryField screenshotField = ImmutableBinaryField.builder().name("Screenshot").variable("screenshot").build();
            final ImmutableTextField   titleField      = ImmutableTextField.builder().name("title").variable("title").build();
            final ImmutableTextField   urlField        = ImmutableTextField.builder().name("url").variable("url").required(true).indexed(true).unique(false).build();
            final ImmutableTextField   orderField      = ImmutableTextField.builder().name("order").dataType(DataTypes.INTEGER).variable("order").indexed(true).build();

            newFields.add(screenshotField);
            newFields.add(titleField);
            newFields.add(urlField);
            newFields.add(orderField);
            final ContentType savedContentType = contentTypeAPI.save(simpleContentType, newFields, null);

            final Set<String> workflowIds = new HashSet<>();
            workflowIds.add(WorkflowAPI.SYSTEM_WORKFLOW_ID);

            APILocator.getWorkflowAPI().saveSchemeIdsForContentType(savedContentType, workflowIds);
            Logger.debug(ContentTypeInitializer.class, "dotFavoritePage CT Saved.");
        } catch (DotDataException | DotSecurityException e) {

            Logger.warnAndDebug(this.getClass(), e);
        }
    }

    /**
     * Makes favorite pages readable and editable only by their owner. The content type gets CMS
     * Owner permissions for itself and a row its contentlets inherit, since favorites are saved on
     * the System Host and inherit from their content type. When either row is missing, or another
     * role has a row on the type, the type's permissions are replaced with those two rows. The first
     * repair on an install also resets the favorites' own rows so they inherit them, and records
     * that in the system table under {@link #FAVORITE_PAGE_PERMISSIONS_RESET_KEY}, in the same
     * transaction; favorites not on the System Host, which inherit from their site or folder rather
     * than the type, get their own CMS Owner rows in that reset. Later repairs leave rows added to
     * individual favorites alone. Every repair
     * queues the type's favorites for reindex. If any step of the repair fails, the whole repair rolls back,
     * including the type's rows, the error is logged and the next start tries again. Each repair
     * is logged. Runs on every start, so it also repairs an existing install. Turned off with
     * {@code DO_DEFAULT_PAGE_PERMISSIONS=false}. The check and the repair run in one transaction
     * that first takes a database lock, so nodes starting together take turns: a node that finds
     * another node repairing waits for it to commit or roll back, then checks the rows it left and
     * repairs them itself if they are still wrong. Once a repair commits, the permission cache is
     * cleared and the favorites are queued for reindex again, and both are done once more a short
     * time later, so that rows a lookup read before the commit and cached after the first clear
     * neither stay cached nor stay in the index.
     *
     * @param savedContentType the favorite page content type
     */
    private void checkDefaultPermissions(final ContentType savedContentType) {

        if (doDefaultPagePermissions.get()) {

            try {

                final String repair = LocalTransaction.wrapReturn(() -> this.repairDefaultPermissions(savedContentType));
                if (null != repair) {
                    // the repair evicts cached rows before it commits, so a read on this or
                    // another node can cache the old rows again until then; flush them now that
                    // the new rows are committed, and reindex from what is read after the flush
                    this.clearPermissionCacheAndReindex(savedContentType);
                    // a lookup that read the old rows before the commit caches them with a plain
                    // put once its query returns, which can land after the clear above, and a
                    // reindex in between indexes them; do both again once such lookups are done
                    // prune: time-bounded, a lookup stalled longer than the delay can still cache
                    // the old rows; fence the cache put itself if that is ever seen
                    DotConcurrentFactory.getScheduledThreadPoolExecutor().schedule(
                            () -> this.clearPermissionCacheAndReindex(savedContentType),
                            PERMISSION_CACHE_RECLEAR_SECONDS, TimeUnit.SECONDS);
                    Logger.info(this, repair);
                }
            } catch (final Throwable e) {

                Logger.error(this, "Favorite pages: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Clears both permission cache groups, the rows and the read decisions, then queues every
     * favorite for reindex, so each index document is rebuilt from permissions read after the
     * clear. The repair already queues them inside its transaction, but that reindex can run
     * before the cache is cleared and index the old permissions. Failures are logged, since this
     * also runs on a scheduler thread where they would otherwise be lost.
     *
     * @param contentType the favorite page content type
     */
    private void clearPermissionCacheAndReindex(final ContentType contentType) {

        CacheLocator.getPermissionCache().clearCache();
        try {

            APILocator.getReindexQueueAPI().addStructureReindexEntries(contentType);
        } catch (final Throwable e) {

            Logger.error(this, "Favorite pages: could not queue the favorites for reindex: " + e.getMessage(), e);
        }
    }

    /**
     * Checks the favorite page content type permissions and repairs them when needed, as described
     * on {@link #checkDefaultPermissions(ContentType)}. Must run inside a transaction: it takes a
     * database lock held until that transaction ends, and reads the type's rows from the database
     * rather than from the cache, so it sees what another node's repair committed.
     *
     * @param savedContentType the favorite page content type
     * @return the message to log once the repair commits, or {@code null} if nothing was repaired
     * @throws DotDataException     if the permissions cannot be read or saved
     * @throws DotSecurityException if the permissions cannot be saved
     */
    private String repairDefaultPermissions(final ContentType savedContentType)
            throws DotDataException, DotSecurityException {

        this.acquireRepairLock();
        APILocator.getPermissionAPI().removePermissionableFromCache(savedContentType.getPermissionId());

        // Add CMS Owner Permissions
        final int permissionType = PermissionAPI.PERMISSION_USE | PermissionAPI.PERMISSION_EDIT |
                PermissionAPI.PERMISSION_PUBLISH | PermissionAPI.PERMISSION_EDIT_PERMISSIONS;
        final Role backendRole = APILocator.getRoleAPI().loadCMSOwnerRole();
        // Until the favorites have been reset once they may carry the old grants. After that a
        // favorite's own rows were added on purpose, so later repairs fix the type only. The type
        // rows cannot tell the two apart: the previous initializer also wrote both owner rows.
        final boolean resetFavorites = !this.favoritesWereReset();
        if (!resetFavorites
                && APILocator.getPermissionAPI().doesRoleHavePermission(savedContentType, permissionType, backendRole)
                && this.contentletsInheritPermission(savedContentType, permissionType, backendRole)
                && this.onlyRoleHasRows(savedContentType, backendRole)) {

            return null;
        }

        final int favoritesWithOwnRows = resetFavorites ? this.countFavoritesWithOwnRows(savedContentType) : 0;
        final List<Permission> newSetOfPermissions = new ArrayList<>();
        // this is the individual permission
        newSetOfPermissions.add(new Permission(savedContentType.getPermissionId(), backendRole.getId(), permissionType, true));
        // this is the inheritance permission
        newSetOfPermissions.add(new Permission(Contentlet.class.getCanonicalName(), savedContentType.getPermissionId(),  backendRole.getId(), permissionType, true));
        // same transaction as the check: if anything below fails, the type rows and the marker
        // roll back too and the next start repairs it again, favorites included.
        // removePermissions also queues every favorite of the type for reindex, so the index
        // picks up the new rows whether or not resetFavorites is set; the caller queues them
        // again after it clears the permission cache, in case this reindex reads cached old rows.
        APILocator.getPermissionAPI().removePermissions(savedContentType);
        APILocator.getPermissionAPI().assignPermissions(newSetOfPermissions, savedContentType, APILocator.systemUser(), false);
        // reset here rather than through ResetPermissionsJob, which needs a logged-in
        // request user and has none at startup
        int favoritesOffSystemHost = 0;
        if (resetFavorites) {
            APILocator.getPermissionAPI().resetPermissionsUnder(savedContentType);
            favoritesOffSystemHost = this.giveOwnerRowsToFavoritesOffSystemHost(savedContentType, permissionType, backendRole);
            APILocator.getSystemAPI().getSystemTable().set(FAVORITE_PAGE_PERMISSIONS_RESET_KEY, "true");
        }
        return resetFavorites
                ? "Favorite pages: reset the content type permissions to CMS Owner only, removed the own permissions of "
                        + favoritesWithOwnRows + " favorite(s) and gave " + favoritesOffSystemHost
                        + " favorite(s) outside the System Host their own CMS Owner permissions"
                : "Favorite pages: reset the content type permissions to CMS Owner only; favorites kept their own permissions";
    }

    /**
     * Gives every favorite that is not on the System Host its own CMS Owner rows. The dialog saves
     * favorites on the System Host, where they inherit from the content type, but an API call can
     * save one on a site or in a folder, and then it inherits from that site or folder instead.
     * Once the reset has removed its own rows, such a favorite would take the site's permissions,
     * which can let anyone read it; its own rows keep it readable only by its owner.
     *
     * @param contentType    the favorite page content type
     * @param permissionType the permission bits to give CMS Owner
     * @param role           the CMS Owner role
     * @return how many favorites were given their own rows
     * @throws DotDataException     if the favorites cannot be read or their permissions saved
     * @throws DotSecurityException if the permissions cannot be saved
     */
    private int giveOwnerRowsToFavoritesOffSystemHost(final ContentType contentType, final int permissionType,
                                                      final Role role) throws DotDataException, DotSecurityException {

        final List<Map<String, Object>> rows = new DotConnect().setSQL(FAVORITES_OFF_SYSTEM_HOST_SQL)
                .addParam(contentType.inode()).addParam(Host.SYSTEM_HOST).addParam("/")
                .loadObjectResults();
        int count = 0;
        for (final Map<String, Object> row : rows) {

            final Contentlet favorite = APILocator.getContentletAPI()
                    .findContentletByIdentifierAnyLanguage(String.valueOf(row.get("id")), true);
            if (null != favorite) {
                APILocator.getPermissionAPI().assignPermissions(List.of(
                        new Permission(favorite.getPermissionId(), role.getId(), permissionType, true)),
                        favorite, APILocator.systemUser(), false);
                count++;
            }
        }
        return count;
    }

    /**
     * Takes the database lock that makes nodes starting together run the favorite permission
     * check and repair one at a time. It is a Postgres transaction-level advisory lock, so it is
     * released when the calling transaction commits or rolls back, or when the holder's
     * connection closes. There is no timeout on purpose: a node waits for another node's repair
     * to end and then checks the rows it left, rather than giving up and leaving them unchecked
     * until the next start if that repair rolls back.
     *
     * @throws DotDataException if the lock cannot be taken
     */
    private void acquireRepairLock() throws DotDataException {

        new DotConnect().setSQL(REPAIR_LOCK_SQL)
                .addParam(FAVORITE_PAGE_PERMISSIONS_LOCK)
                .loadObjectResults();
    }

    /**
     * Tells whether a repair has already reset the favorites' own permission rows, which happens
     * once per install. Reads the system table row directly rather than through its cache, so a
     * node waiting on the repair lock sees the marker another node's repair committed.
     *
     * @return {@code true} if the favorites were reset by an earlier repair
     * @throws DotDataException if the system table cannot be read
     */
    private boolean favoritesWereReset() throws DotDataException {

        return new DotConnect().setSQL(COUNT_RESET_MARKER_SQL)
                .addParam(FAVORITE_PAGE_PERMISSIONS_RESET_KEY).getInt("total") > 0;
    }

    /**
     * Tells whether the contentlets of a content type inherit the given permission bits for a
     * role from it. Favorite pages are saved on the System Host, so without this row they inherit
     * from the System Host instead, whatever the type's own permissions are.
     *
     * @param contentType    the content type the contentlets inherit from
     * @param permissionType the permission bits the contentlets must inherit
     * @param role           the role the bits are granted to
     * @return {@code true} if a row the contentlets inherit grants every bit to the role
     * @throws DotDataException if the permissions cannot be read
     */
    private boolean contentletsInheritPermission(final ContentType contentType, final int permissionType,
                                                 final Role role) throws DotDataException {

        final List<Permission> inheritable = APILocator.getPermissionAPI()
                .getInheritablePermissions(contentType, true);
        return null != inheritable && inheritable.stream().anyMatch(permission ->
                Contentlet.class.getCanonicalName().equals(permission.getType())
                        && role.getId().equals(permission.getRoleId())
                        && (permission.getPermission() & permissionType) == permissionType);
    }

    /**
     * Counts the contentlets of a content type that have permission rows of their own, which a
     * reset of the permissions under the type removes.
     *
     * @param contentType the content type whose contentlets are counted
     * @return how many of its contentlets have their own rows
     * @throws DotDataException if the rows cannot be read
     */
    private int countFavoritesWithOwnRows(final ContentType contentType) throws DotDataException {

        return new DotConnect().setSQL(COUNT_FAVORITES_WITH_OWN_ROWS_SQL)
                .addParam(contentType.inode()).getInt("total");
    }

    /**
     * Tells whether every permission row on a content type, its own and the ones its contentlets
     * inherit, belongs to the given role. A row for any other role would let favorites be read by
     * someone other than their owner.
     *
     * @param contentType the content type whose rows are checked
     * @param role        the only role the rows may belong to
     * @return {@code true} if no row on the type belongs to another role
     * @throws DotDataException if the permissions cannot be read
     */
    private boolean onlyRoleHasRows(final ContentType contentType, final Role role) throws DotDataException {

        final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
        final List<Permission> rows = new ArrayList<>(permissionAPI.getPermissions(contentType, true, true));
        final List<Permission> inheritable = permissionAPI.getInheritablePermissions(contentType, true);
        if (null != inheritable) {
            rows.addAll(inheritable);
        }
        return rows.stream().allMatch(permission -> role.getId().equals(permission.getRoleId()));
    }
}
