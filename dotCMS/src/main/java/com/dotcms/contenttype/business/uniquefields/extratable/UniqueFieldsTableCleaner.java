package com.dotcms.contenttype.business.uniquefields.extratable;

import com.dotcms.contenttype.business.uniquefields.UniqueFieldValidationStrategyResolver;
import com.dotcms.contenttype.model.event.ContentTypeDeletedEvent;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.event.FieldDeletedEvent;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.system.event.local.model.Subscriber;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DeleteContentletVersionInfoEvent;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;

/**
 * Responsible for maintaining the unique_fields table, which is used for database unique field validation.
 * This table must be cleaned up under the following circumstances:
 *
 * - Contentlet or its variant deletion: Remove all related {@link Contentlet} entries.
 * - Host deletion with cascading Contentlet removal: Remove all related {@link Contentlet} entries.
 * - Push Remove process: When a Contentlet is sent to a receiver, remove all associated {@link Contentlet} entries.
 * - Contentlet unpublish:
 *     - If the LIVE and WORKING versions differ, remove the LIVE entry.
 *     - If the LIVE and WORKING versions are the same, retain the entry.
 * - ContentType with unique fields deletion: Remove all related entries.
 * - Unique field deletion: Clean up entries for the deleted field.
 *
 *
 * @see DBUniqueFieldValidationStrategy
 */
@Dependent
public class UniqueFieldsTableCleaner {

    final UniqueFieldValidationStrategyResolver uniqueFieldValidationStrategyResolver;

    @Inject
    public UniqueFieldsTableCleaner(final UniqueFieldValidationStrategyResolver uniqueFieldValidationStrategyResolver){
        this.uniqueFieldValidationStrategyResolver = uniqueFieldValidationStrategyResolver;
    }

    /**
     /**
     * Listens for the deletion of a {@link Contentlet} and performs the following actions:
     *
     * - If {@link DeleteContentletVersionInfoEvent#isDeleteAllVariant()} is true:
     *   Delete all records associated with the {@link Contentlet}'s
     *   {@link com.dotmarketing.portlets.languagesmanager.model.Language}
     *   and {@link com.dotcms.variant.model.Variant}.
     *
     * - If {@link DeleteContentletVersionInfoEvent#isDeleteAllVariant()} is false:
     *   Delete all records associated with the {@link Contentlet}'s
     *   {@link com.dotmarketing.portlets.languagesmanager.model.Language}
     *   across all {@link com.dotcms.variant.model.Variant} instances.
     *
     * @param event
     * @throws DotDataException
     */
    @Subscriber
    public void cleanUpAfterDeleteContentlet(final DeleteContentletVersionInfoEvent event) throws DotDataException {

        final Contentlet contentlet = event.getContentlet();

        try {
            final ContentType contentType = APILocator.getContentTypeAPI(APILocator.systemUser())
                    .find(contentlet.getContentTypeId());

            Logger.debug(UniqueFieldsTableCleaner.class, "Cleaning up Content Type with id " + contentlet.getContentTypeId());

            boolean hasUniqueField = hasUniqueField(contentType);

            Logger.info(UniqueFieldsTableCleaner.class, "Has unique field: " + hasUniqueField);

            if (hasUniqueField) {
                uniqueFieldValidationStrategyResolver.get().cleanUp(contentlet, event.isDeleteAllVariant());
            }
        } catch (DotSecurityException e) {
            throw new DotRuntimeException(e);
        } catch (Exception e) {
            Logger.error(UniqueFieldsTableCleaner.class, e);
            throw new DotDataException(e);
        }
    }

    private static boolean hasUniqueField(ContentType contentType) {
        return contentType.fields().stream().anyMatch(Field::unique);
    }

    /**
     * Listen when a Field is deleted and if this is a Unique Field then delete all the register in
     * unique_fields table for this Field
     *
     * @param event
     *
     * @throws DotDataException
     */
    @Subscriber
    public void cleanUpAfterDeleteUniqueField(final FieldDeletedEvent event) throws DotDataException {
        final Field deletedField = event.getField();

        if (deletedField.unique()) {
            uniqueFieldValidationStrategyResolver.get().cleanUp(deletedField);
        }
    }

    /**
     * Listen when a {@link ContentType} is deleted and if this has at least one Unique Field then delete all the register in
     * unique_fields table for this {@link ContentType}
     *
     * @param event
     *
     * @throws DotDataException
     */
    @Subscriber
    public void cleanUpAfterDeleteContentType(final ContentTypeDeletedEvent contentTypeDeletedEvent) throws DotDataException {
        final ContentType contentType = contentTypeDeletedEvent.getContentType();

        boolean hasUniqueField = hasUniqueField(contentType);

        if (hasUniqueField) {
            uniqueFieldValidationStrategyResolver.get().cleanUp(contentType);
        }
    }
}
