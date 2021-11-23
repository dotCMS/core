package com.dotcms.content.model.hydration;

import static com.dotcms.util.ReflectionUtils.setValue;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.content.model.FieldValueBuilder;
import com.dotcms.contenttype.model.field.Field;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ResourceLink;
import javax.servlet.http.HttpServletRequest;

public class ResourceLinkDelegate implements HydrationDelegate {

    @Override
    public FieldValueBuilder hydrate(final FieldValueBuilder builder, final Field field,
            final Contentlet contentlet, String propertyName)
            throws DotDataException, DotSecurityException {
        final HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        final ResourceLink resourceLink = new ResourceLink.ResourceLinkBuilder().build(request, APILocator.systemUser(), contentlet, field.variable());
        setValue(builder, propertyName, resourceLink.getConfiguredImageURL());
        return builder;
    }

}
