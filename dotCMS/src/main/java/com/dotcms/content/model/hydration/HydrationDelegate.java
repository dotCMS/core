package com.dotcms.content.model.hydration;

import com.dotcms.content.model.FieldValueBuilder;
import com.dotcms.contenttype.model.field.Field;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;

public interface HydrationDelegate {

    FieldValueBuilder hydrate(FieldValueBuilder builder, Field field, Contentlet contentlet,
            String propertyName)
            throws DotDataException, DotSecurityException;

}
