package com.dotcms.contenttype.business;

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
import java.util.Set;

/**
 * Initialiaze content types
 * @author jsanca
 */
public class ContentTypeInitializer implements DotInitializer {

    public static final String LEGACY_FAVORITE_PAGE_VAR_NAME = "favoritePage";
    public static final String FAVORITE_PAGE_VAR_NAME = "dotFavoritePage";

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
     * role has a row on the type, the type's permissions are replaced with those two rows. When the
     * row the contentlets inherit was missing, which is the state before this repair first ran, the
     * favorites' own rows are also reset so they inherit them, in the same transaction; later
     * repairs leave rows added to individual favorites alone. Each repair is logged. Runs on every
     * start, so it also repairs an existing install. Turned off with
     * {@code DO_DEFAULT_PAGE_PERMISSIONS=false}.
     *
     * @param savedContentType the favorite page content type
     */
    private void checkDefaultPermissions(final ContentType savedContentType) {

        if (doDefaultPagePermissions.get()) {

            try {

                // Add CMS Owner Permissions
                final int permissionType = PermissionAPI.PERMISSION_USE | PermissionAPI.PERMISSION_EDIT |
                        PermissionAPI.PERMISSION_PUBLISH | PermissionAPI.PERMISSION_EDIT_PERMISSIONS;
                final Role backendRole = APILocator.getRoleAPI().loadCMSOwnerRole();
                final boolean contentletsInherit = this.contentletsInheritPermission(savedContentType, permissionType, backendRole);
                if (!APILocator.getPermissionAPI().doesRoleHavePermission(savedContentType, permissionType, backendRole)
                        || !contentletsInherit
                        || !this.onlyRoleHasRows(savedContentType, backendRole)) {

                    // Only a type this repair has not run on yet lacks the row favorites inherit;
                    // its favorites may carry the old grants. Once it has run, a favorite's own
                    // rows were added on purpose, so later repairs fix the type only.
                    final boolean resetFavorites = !contentletsInherit;
                    final int favoritesWithOwnRows = resetFavorites ? this.countFavoritesWithOwnRows(savedContentType) : 0;
                    final List<Permission> newSetOfPermissions = new ArrayList<>();
                    // this is the individual permission
                    newSetOfPermissions.add(new Permission(savedContentType.getPermissionId(), backendRole.getId(), permissionType, true));
                    // this is the inheritance permission
                    newSetOfPermissions.add(new Permission(Contentlet.class.getCanonicalName(), savedContentType.getPermissionId(),  backendRole.getId(), permissionType, true));
                    // one transaction, so a failed reset leaves the type rows unchanged and the
                    // next start repairs it again
                    LocalTransaction.wrap(() -> {
                        APILocator.getPermissionAPI().removePermissions(savedContentType);
                        APILocator.getPermissionAPI().assignPermissions(newSetOfPermissions, savedContentType, APILocator.systemUser(), false);
                        // reset here rather than through ResetPermissionsJob, which needs a logged-in
                        // request user and has none at startup
                        if (resetFavorites) {
                            APILocator.getPermissionAPI().resetPermissionsUnder(savedContentType);
                        }
                    });
                    // the repair evicts cached rows before it commits, so a read on this or another
                    // node can cache the old rows again until then; flush both permission cache
                    // groups, the rows and the read decisions, now that the new rows are committed
                    CacheLocator.getPermissionCache().clearCache();
                    Logger.info(this, resetFavorites
                            ? "Favorite pages: reset the content type permissions to CMS Owner only and removed the own permissions of "
                                    + favoritesWithOwnRows + " favorite(s)"
                            : "Favorite pages: reset the content type permissions to CMS Owner only; favorites kept their own permissions");
                }
            } catch (DotDataException | DotSecurityException e) {

                Logger.error(this, "Favorite pages: " + e.getMessage(), e);
            }
        }
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

        return new DotConnect().setSQL("select count(distinct inode_id) as total from permission where inode_id in"
                        + " (select identifier from contentlet where structure_inode = ?)")
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
