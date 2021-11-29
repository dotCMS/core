package com.dotcms.content.model.hydration;

import static com.dotcms.content.model.hydration.HydrationUtils.findLinkedBinary;
import static com.dotcms.util.ReflectionUtils.setValue;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.content.model.FieldValueBuilder;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.ImageField;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ResourceLink;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;

public class ResourceLinkDelegate implements HydrationDelegate {

    @Override
    public FieldValueBuilder hydrate(final FieldValueBuilder builder, final Field field,
            final Contentlet contentlet, String propertyName)
            throws DotDataException, DotSecurityException {

        ResourceLink resourceLink = null;
        final HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();

        if(field instanceof ImageField){
            final Optional<Contentlet> fileAsContentOptional = findLinkedBinary(contentlet,(ImageField) field);
            if (fileAsContentOptional.isPresent()) {
                final Contentlet fileAsset = fileAsContentOptional.get();
                resourceLink = new ResourceLink.ResourceLinkBuilder().build(request, APILocator.systemUser(), fileAsset, "fileAsset");
            }
        } else {
            resourceLink = new ResourceLink.ResourceLinkBuilder().build(request, APILocator.systemUser(), contentlet, field.variable());
        }
        setValue(builder, propertyName,  resourceLink != null ? resourceLink.getConfiguredImageURL() : "unknown");
        return builder;
    }

}
