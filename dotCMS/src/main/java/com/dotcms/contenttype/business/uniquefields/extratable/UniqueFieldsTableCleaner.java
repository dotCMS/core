package com.dotcms.contenttype.business.uniquefields.extratable;

import com.dotcms.contenttype.business.uniquefields.UniqueFieldValidationStrategyResolver;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.system.event.local.model.Subscriber;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DeleteContentletVersionInfoEvent;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class UniqueFieldsTableCleaner {

    final UniqueFieldValidationStrategyResolver uniqueFieldValidationStrategyResolver;

    @Inject
    public UniqueFieldsTableCleaner(final UniqueFieldValidationStrategyResolver uniqueFieldValidationStrategyResolver){
        this.uniqueFieldValidationStrategyResolver = uniqueFieldValidationStrategyResolver;
    }

    @Subscriber
    public void cleanUpAfterDeleteContentlet(final DeleteContentletVersionInfoEvent event) throws DotDataException {

        final Contentlet contentlet = event.getContentlet();

        try {
            final ContentType contentType = APILocator.getContentTypeAPI(APILocator.systemUser())
                    .find(contentlet.getContentTypeId());

            boolean hasUniqueField = contentType.fields().stream().anyMatch(Field::unique);

            if (hasUniqueField) {
                uniqueFieldValidationStrategyResolver.get().cleanUp(contentlet, event.isDeleteAllVariant());
            }
        } catch (DotSecurityException e) {
            throw new DotRuntimeException(e);
        }
    }
}
