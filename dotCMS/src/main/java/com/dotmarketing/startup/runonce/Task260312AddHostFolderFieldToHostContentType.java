package com.dotmarketing.startup.runonce;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.business.FieldAPI;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.HostFolderField;
import com.dotcms.contenttype.model.field.ImmutableHostFolderField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;
import io.vavr.control.Try;

/**
 * Adds the {@code parentHost} {@link HostFolderField} to the Host content type if it is not
 * already present. This field enables nestable hosts in dotCMS, allowing a host to designate
 * another host or folder as its parent for hierarchical URL resolution and permission inheritance.
 *
 * <p>The field is added as a non-required, indexed, non-fixed field so that existing hosts remain
 * unaffected (blank = top-level host) and the field can be managed normally through the UI.
 *
 * @author dotCMS
 * @since Mar 12th, 2026
 */
public class Task260312AddHostFolderFieldToHostContentType implements StartupTask {

    static final String PARENT_HOST_FIELD_VAR  = "parentHost";
    static final String PARENT_HOST_FIELD_NAME = "Parent Host or Folder";

    @Override
    public boolean forceRun() {
        try {
            final ContentType hostType = getHostContentType();
            if (hostType == null) {

                throw new DotRuntimeException("Host content type not found");
            }
            final boolean fieldMissing = hostType.fields().stream()
                    .noneMatch(f -> f instanceof HostFolderField
                            || PARENT_HOST_FIELD_VAR.equalsIgnoreCase(f.variable()));
            if (!fieldMissing) {
                Logger.debug(this, "'" + PARENT_HOST_FIELD_VAR
                        + "' field already exists on Host content type – skipping task.");
            }
            return fieldMissing;
        } catch (final Exception e) {
            Logger.error(this, "Error checking Host content type for HostFolderField: "
                    + e.getMessage(), e);
            throw new DotRuntimeException("Error checking Host content type for HostFolderField: ");
        }
    }

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        final ContentType hostType = getHostContentType();
        if (hostType == null) {
            throw new DotRuntimeException("Host content type not found – cannot add field.");
        }

        // Double-check that the field is still missing inside the transactional boundary
        final boolean alreadyPresent = hostType.fields().stream()
                .anyMatch(f -> f instanceof HostFolderField
                        || PARENT_HOST_FIELD_VAR.equalsIgnoreCase(f.variable()));
        if (alreadyPresent) {
            Logger.info(this, "'" + PARENT_HOST_FIELD_VAR
                    + "' field already exists on Host content type – nothing to do.");
            return;
        }

        // Determine next sort order so the new field appears at the end
        final int sortOrder = hostType.fieldMap().get(Host.HOST_NAME_KEY).sortOrder() + 1;

        final Field parentHostField = ImmutableHostFolderField.builder()
                .name(PARENT_HOST_FIELD_NAME)
                .variable(PARENT_HOST_FIELD_VAR)
                .contentTypeId(hostType.id())
                .required(false)
                .indexed(true)
                .defaultValue(Host.SYSTEM_HOST)
                .fixed(false)
                .sortOrder(sortOrder)
                .build();

        final FieldAPI fieldAPI = APILocator.getContentTypeFieldAPI();
        try {
            fieldAPI.save(parentHostField, APILocator.systemUser());
            Logger.info(this, "Successfully added '" + PARENT_HOST_FIELD_VAR
                    + "' HostFolderField to the Host content type.");
        } catch (final DotSecurityException e) {
            throw new DotRuntimeException(
                    "Permission denied while adding '" + PARENT_HOST_FIELD_VAR
                            + "' field to Host content type: " + e.getMessage(), e);
        } finally {
            // Ensure the content-type cache is cleared so any subsequent read picks up the new field
            CacheLocator.getContentTypeCache2().clearCache();
        }
    }

    // -----------------------------------------------------------------------
    // helpers
    // -----------------------------------------------------------------------

    private ContentType getHostContentType() {
        final ContentTypeAPI contentTypeAPI =
                APILocator.getContentTypeAPI(APILocator.systemUser());
        return Try.of(() -> contentTypeAPI.find(Host.HOST_VELOCITY_VAR_NAME)).getOrNull();
    }
}
