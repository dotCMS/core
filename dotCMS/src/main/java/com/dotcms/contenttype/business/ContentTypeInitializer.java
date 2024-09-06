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
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.quartz.job.ResetPermissionsJob;
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

    private void checkDefaultPermissions(final ContentType savedContentType) {

        if (doDefaultPagePermissions.get()) {

            try {

                // Add CMS Owner Permissions
                final int permissionType = PermissionAPI.PERMISSION_USE | PermissionAPI.PERMISSION_EDIT |
                        PermissionAPI.PERMISSION_PUBLISH | PermissionAPI.PERMISSION_EDIT_PERMISSIONS;
                final Role backendRole = APILocator.getRoleAPI().loadCMSOwnerRole();
                if (!APILocator.getPermissionAPI().doesRoleHavePermission(savedContentType, permissionType, backendRole)) {

                    // remove all current permissions
                    APILocator.getPermissionAPI().removePermissions(savedContentType);

                    Logger.info(this, "Adding default permissions to the Favorite Page Content Type...");
                    final List<Permission> newSetOfPermissions = new ArrayList<>();
                    // this is the individual permission
                    newSetOfPermissions.add(new Permission(savedContentType.getPermissionId(), backendRole.getId(), permissionType, true));
                    // this is the inheritance permission
                    newSetOfPermissions.add(new Permission(Contentlet.class.getCanonicalName(), savedContentType.getPermissionId(),  backendRole.getId(), permissionType, true));
                    APILocator.getPermissionAPI().assignPermissions(newSetOfPermissions, savedContentType, APILocator.systemUser(), false);
                    ResetPermissionsJob.triggerJobImmediately(savedContentType);
                }
            } catch (DotDataException | DotSecurityException e) {

                Logger.error(this, "Favorite pages: " + e.getMessage(), e);
            }
        }
    }
}
