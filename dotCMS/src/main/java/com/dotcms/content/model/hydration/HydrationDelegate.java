package com.dotcms.content.model.hydration;

import com.dotcms.content.model.FieldValueBuilder;
import com.dotcms.contenttype.model.field.Field;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;

/**
 * This interface is intended to build and inject a value via FieldValueBuilder in a contentlet
 * What this means is we want to inject into a FieldValue object an attribute which is calculated (e.g. the metadata)
 * For that purpose we use a FieldValueBuilder. Implementations of this interface should be aware of how to accomplish such task.
 */
public interface HydrationDelegate {

    /**
     * When a Field is annotated this method gets called from the contentletToJsonAPI
     * @param builder FieldValueBuilder
     * @param field The Field we're looking at
     * @param contentlet The Source Contentlet
     * @param propertyName The property we want to inject
     * @return FieldValueBuilder with the new calculated values
     * @throws DotDataException
     * @throws DotSecurityException
     */
    FieldValueBuilder hydrate(FieldValueBuilder builder, Field field, Contentlet contentlet,
            String propertyName)
            throws DotDataException, DotSecurityException;

}
