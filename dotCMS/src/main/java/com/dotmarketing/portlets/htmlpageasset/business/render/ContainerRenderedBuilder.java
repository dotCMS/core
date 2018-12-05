package com.dotmarketing.portlets.htmlpageasset.business.render;


import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.velocity.context.Context;

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

/**
 * Builder of {@link ContainerRendered}
 */
public class ContainerRenderedBuilder {


    public Collection<? extends ContainerRaw> getContainers(final HTMLPageAsset page, final PageMode mode)
            throws DotSecurityException, DotDataException {
        return getContainersRendered(page, null, mode);
    }


    public Collection<? extends ContainerRaw> getContainersRendered(HTMLPageAsset page, final Context velocityContext, PageMode mode)
            throws DotSecurityException, DotDataException {

   
        PageContextBuilder pcb = new PageContextBuilder(page, APILocator.systemUser(), mode);


        if (velocityContext == null) {
            return pcb.getContainersRaw();
        }



        return pcb.getContainersRaw().stream().map(cRaw -> {
            try {
                Map<String, String> uuidsRendered = render(velocityContext, mode, cRaw);
                return new ContainerRendered(cRaw, uuidsRendered);
            } catch (Exception e) {
                // if the container does not exists or is not valid for the mode, returns null to be filtrated
                return null;

            }
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }



    private Map<String, String> render(Context velocityContext, PageMode mode, ContainerRaw cRaw) {

        Map<String, String> rendered = Maps.newHashMap();
        for (String uuid : cRaw.getContentlets().keySet()) {
            final VelocityResourceKey key = new VelocityResourceKey(cRaw.getContainer(), uuid, mode);
            try {
                rendered.put(uuid, VelocityUtil.getInstance().mergeTemplate(key.path, velocityContext));
            } catch (Exception e) {
                Logger.warn(this.getClass(), e.getMessage());
            }
        }
        return rendered;
    }
}
