package com.dotcms.rendering.velocity.services;


import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.repackage.com.google.common.collect.Lists;
import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.MultiTreeAPI;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.business.render.ContainerRaw;
import com.dotmarketing.portlets.htmlpageasset.business.render.page.HTMLPageAssetRenderedBuilder;
import com.dotmarketing.portlets.htmlpageasset.business.render.page.PageContent;
import com.dotmarketing.portlets.htmlpageasset.business.render.page.PageRenderContext;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.personas.model.IPersona;
import com.dotmarketing.tag.business.TagAPI;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.PageMode;
import com.google.common.collect.Maps;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.*;

/**
 * Util class for rendering a Page
 */
public final class PageRenderUtil implements Serializable {
    private static final long serialVersionUID = 1L;


    private static final MultiTreeAPI  multiTreeAPI  = APILocator.getMultiTreeAPI();
    private static final TagAPI        tagAPI        = APILocator.getTagAPI();

    private PageRenderUtil(){}


    /**
     * Return the Page's {@link ContainerRaw}
     * @return
     */
    public static List<ContainerRaw> getContainersRaw(
            final IHTMLPage htmlPage,
            final IPersona persona,
            final PageRenderContext pageRenderContext) throws DotDataException, DotSecurityException {

        final PageMode mode = pageRenderContext.getMode();

        final String personaId = persona == null ? MultiTree.DOT_PERSONALIZATION_DEFAULT
                : HTMLPageAssetRenderedBuilder.getPersonaTag(persona);
        final PageContent pageContents = getPageContent(mode, htmlPage);
        final List<ContainerRaw> raws = Lists.newArrayList();

        for (final Container container: pageContents.getContainers()) {

            final List<ContainerStructure> containerStructures = APILocator.getContainerAPI().getContainerStructures(container);
            raws.add(new ContainerRaw(container, containerStructures, pageContents
                    .getContentsByUUID(container, pageRenderContext, personaId)));
        }

        return raws;
    }

    private static PageContent getPageContent(
            final PageMode mode,
            final IHTMLPage htmlPage) throws DotDataException, DotSecurityException {

        final HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        final boolean live =
                request != null && request.getSession(false) != null && request.getSession().getAttribute("tm_date") != null ?
                        false :
                        mode.showLive;
        return multiTreeAPI.getPageMultiTrees(htmlPage, live);
    }

    public static  List<Tag> getPageFoundTags(
            final IHTMLPage htmlPage,
            final PageRenderContext pageRenderContext) throws DotSecurityException, DotDataException {

        final List<Tag> pageFoundTags = Lists.newArrayList();
        final boolean accurateTags = Config.getBooleanProperty("ACCRUE_TAGS_IN_CONTENTS_ON_PAGE", false);

        if (!accurateTags) {
            return pageFoundTags;
        }

        final Collection<Contentlet> contents = getPageContent(pageRenderContext.getMode(), htmlPage)
                .getContents(pageRenderContext);

        for (final Contentlet contentlet : contents) {
            final List<Tag> contentletFoundTags = tagAPI.getTagsByInode(contentlet.getInode());

            if (contentletFoundTags != null) {
                pageFoundTags.addAll(contentletFoundTags);
            }
        }

        return pageFoundTags;
    }

    public static int getPersonalizationNumber(final IHTMLPage htmlPage) {
        try {
            return getPageContent(PageMode.PREVIEW_MODE, htmlPage).getPersonalizations().size();
        } catch(DotSecurityException | DotDataException e) {
            throw new DotRuntimeException(e);
        }
    }
}
