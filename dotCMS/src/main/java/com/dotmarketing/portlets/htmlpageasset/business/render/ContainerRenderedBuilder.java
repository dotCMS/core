package com.dotmarketing.portlets.htmlpageasset.business.render;


import com.beust.jcommander.internal.Maps;
import com.dotcms.rendering.velocity.services.PageContextBuilder;
import com.dotcms.rendering.velocity.services.VelocityResourceKey;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.VelocityUtil;
import org.apache.velocity.context.Context;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Builder of {@link ContainerRendered}
 */
public class ContainerRenderedBuilder {


    public Collection<? extends ContainerRaw> getContainers(final HTMLPageAsset page, final PageMode mode)
            throws DotSecurityException, DotDataException {
        return getContainersRendered(page, null, mode);
    }


    public Collection<? extends ContainerRaw> getContainersRendered(final HTMLPageAsset page, final Context velocityContext, final PageMode mode)
            throws DotSecurityException, DotDataException {

        final PageContextBuilder pageContextBuilder = new PageContextBuilder(page, APILocator.systemUser(), mode);

        if (velocityContext == null) {
            return pageContextBuilder.getContainersRaw();
        }

        return pageContextBuilder.getContainersRaw().stream().map(containerRaw -> {
            try {
                final Map<String, String> uuidsRendered = render(velocityContext, mode, containerRaw);
                return new ContainerRendered(containerRaw, uuidsRendered);
            } catch (Exception e) {
                // if the container does not exists or is not valid for the mode, returns null to be filtrated
                return null;

            }
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }



    private Map<String, String> render(final Context velocityContext, final PageMode mode, final ContainerRaw containerRaw) {

        final Map<String, String> rendered = Maps.newHashMap();
        for (final String uuid : containerRaw.getContentlets().keySet()) {
            final VelocityResourceKey key = new VelocityResourceKey(containerRaw.getContainer(), uuid.replace("uuid-", ""), mode);
            try {
                rendered.put(uuid, VelocityUtil.getInstance().mergeTemplate(key.path, velocityContext));
            } catch (Exception e) {
                Logger.warn(this.getClass(), e.getMessage());
            }
        }
        return rendered;
    }
}
