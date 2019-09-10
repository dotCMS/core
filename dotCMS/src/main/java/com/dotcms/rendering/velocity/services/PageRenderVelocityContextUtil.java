package com.dotcms.rendering.velocity.services;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.repackage.com.ibm.icu.text.SimpleDateFormat;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.MultiTreeAPI;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.containers.model.FileAssetContainer;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.business.render.page.PageContent;
import com.dotmarketing.portlets.htmlpageasset.business.render.page.PageRenderContext;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.templates.design.bean.ContainerUUID;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.VelocityUtil;
import com.google.common.collect.Maps;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import org.apache.velocity.context.Context;

import javax.servlet.http.HttpServletRequest;
import java.io.StringWriter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.dotmarketing.business.PermissionAPI.*;

public final class PageRenderVelocityContextUtil {

    private static final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
    private static final MultiTreeAPI multiTreeAPI  = APILocator.getMultiTreeAPI();

    private PageRenderVelocityContextUtil(){}

    private static Map<String, Object> populateContext(
            final PageMode mode,
            final IHTMLPage htmlPage,
            final User user) throws DotDataException, DotSecurityException {

        final Map<String, Object> ctxMap = Maps.newHashMap();
        ctxMap.put(mode.name(), Boolean.TRUE);

        // set the page cache var
        if (htmlPage.getCacheTTL() > 0 && LicenseUtil.getLevel() >= LicenseLevel.COMMUNITY.level) {
            ctxMap.put("dotPageCacheDate", new java.util.Date());
            ctxMap.put("dotPageCacheTTL", htmlPage.getCacheTTL());
        }

        final String templateId = htmlPage.getTemplateId();

        final Identifier pageIdent = APILocator.getIdentifierAPI().find(htmlPage.getIdentifier());

        // gets pageChannel for this path
        final java.util.StringTokenizer st = new java.util.StringTokenizer(String.valueOf(pageIdent.getURI()), StringPool.SLASH);
        String pageChannel = null;
        if (st.hasMoreTokens()) {
            pageChannel = st.nextToken();
        }

        final User systemUser = APILocator.getUserAPI().getSystemUser();
        final Template template = mode.showLive ?
                (Template) APILocator.getVersionableAPI().findLiveVersion(templateId, systemUser, false)
                : (Template) APILocator.getVersionableAPI().findWorkingVersion(templateId, systemUser, false);

        // to check user has permission to write on this page
        final List<PublishingEndPoint> receivingEndpoints = APILocator.getPublisherEndPointAPI().getReceivingEndPoints();
        final boolean hasAddChildrenPermOverHTMLPage = permissionAPI.doesUserHavePermission(htmlPage, PERMISSION_CAN_ADD_CHILDREN, user);
        final boolean hasWritePermOverHTMLPage = permissionAPI.doesUserHavePermission(htmlPage, PERMISSION_WRITE, user);
        final boolean hasPublishPermOverHTMLPage = permissionAPI.doesUserHavePermission(htmlPage, PERMISSION_PUBLISH, user);
        final boolean hasRemotePublishPermOverHTMLPage =
                hasPublishPermOverHTMLPage && LicenseUtil.getLevel() >= LicenseLevel.STANDARD.level;
        final boolean hasEndPoints = UtilMethods.isSet(receivingEndpoints) && !receivingEndpoints.isEmpty();
        final boolean canUserWriteOnTemplate = permissionAPI.doesUserHavePermission(template, PERMISSION_WRITE, user)
                && APILocator.getPortletAPI().hasTemplateManagerRights(user);

        ctxMap.put("dotPageMode", mode.name());

        ctxMap.put("ADD_CHILDREN_HTMLPAGE_PERMISSION", hasAddChildrenPermOverHTMLPage);
        ctxMap.put("EDIT_HTMLPAGE_PERMISSION", hasWritePermOverHTMLPage);
        ctxMap.put("PUBLISH_HTMLPAGE_PERMISSION", hasPublishPermOverHTMLPage);
        ctxMap.put("REMOTE_PUBLISH_HTMLPAGE_PERMISSION", hasRemotePublishPermOverHTMLPage);
        ctxMap.put("REMOTE_PUBLISH_END_POINTS", hasEndPoints);
        ctxMap.put("canAddForm", LicenseUtil.getLevel() >= LicenseLevel.STANDARD.level ? true : false);
        ctxMap.put("canViewDiff", LicenseUtil.getLevel() >= LicenseLevel.STANDARD.level ? true : false);
        ctxMap.put("pageChannel", pageChannel);
        ctxMap.put("HTMLPAGE_ASSET_STRUCTURE_TYPE", true);
        ctxMap.put("HTMLPAGE_IS_CONTENT", true);


        ctxMap.put("EDIT_TEMPLATE_PERMISSION", canUserWriteOnTemplate);

        ctxMap.put(ContainerLoader.SHOW_PRE_POST_LOOP, true);
        ctxMap.put("HTMLPAGE_INODE", htmlPage.getInode());
        ctxMap.put("HTMLPAGE_IDENTIFIER", htmlPage.getIdentifier());
        ctxMap.put("HTMLPAGE_FRIENDLY_NAME", UtilMethods.espaceForVelocity(htmlPage.getFriendlyName()));
        ctxMap.put("HTMLPAGE_TITLE", UtilMethods.espaceForVelocity(htmlPage.getTitle()));
        ctxMap.put("TEMPLATE_INODE", templateId);
        ctxMap.put("HTMLPAGE_META", UtilMethods.espaceForVelocity(htmlPage.getMetadata()));
        ctxMap.put("HTMLPAGE_DESCRIPTION", UtilMethods.espaceForVelocity(htmlPage.getSeoDescription()));
        ctxMap.put("HTMLPAGE_KEYWORDS", UtilMethods.espaceForVelocity(htmlPage.getSeoKeywords()));
        ctxMap.put("HTMLPAGE_SECURE", htmlPage.isHttpsRequired());
        ctxMap.put("VTLSERVLET_URI", UtilMethods.encodeURIComponent(pageIdent.getURI()));
        ctxMap.put("HTMLPAGE_REDIRECT", UtilMethods.espaceForVelocity(htmlPage.getRedirect()));
        ctxMap.put("pageTitle", UtilMethods.espaceForVelocity(htmlPage.getTitle()));
        ctxMap.put("friendlyName", UtilMethods.espaceForVelocity(htmlPage.getFriendlyName()));
        ctxMap.put("HTML_PAGE_LAST_MOD_DATE", UtilMethods.espaceForVelocity(htmlPage.getFriendlyName()));
        ctxMap.put("HTMLPAGE_MOD_DATE", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(htmlPage.getModDate()));


        return ctxMap;
    }

    final static String getWidgetPreExecute(
            final IHTMLPage htmlPage,
            final PageMode mode,
            final User user) throws DotSecurityException, DotDataException {

        final Language defaultLanguage = APILocator.getLanguageAPI().getDefaultLanguage();
        final StringBuilder widgetPreExecute = new StringBuilder();
        final PageRenderContext pageRenderContext = new PageRenderContext(mode, defaultLanguage.getId(), user);
        final Collection<Contentlet> contents = getPageContent(mode, htmlPage).getContents(pageRenderContext);

        for (final Contentlet contentlet : contents) {
            final ContentType type = contentlet.getContentType();
            if (type.baseType() == BaseContentType.WIDGET) {
                final com.dotcms.contenttype.model.field.Field field = type.fieldMap().get("widgetPreexecute");
                if (field != null && UtilMethods.isSet(field.values())) {
                    widgetPreExecute.append(field.values());
                }
            }
        }

        return widgetPreExecute.toString();
    }

    public static Context addVelocityContext(
            final IHTMLPage htmlPage,
            final PageRenderContext pageRenderContext,
            final Context incoming) {
        try {
            final Map<String, Object> contextMap = getVelocityContext(htmlPage, pageRenderContext.getMode(),
                    pageRenderContext.getUser());

            for (String key : contextMap.keySet()) {
                incoming.put(key, contextMap.get(key));
            }

            return incoming;
        } catch (DotDataException | DotSecurityException e) {
            throw new DotRuntimeException(e);
        }
    }

    /**
     * Return the Velocity code to set the Page's variables as: contentletList, contentType,
     * totalSize, etc.
     *
     * @return
     */
    public static String asString(
            final IHTMLPage htmlPage,
            final PageMode mode,
            final User user
    ) {
        try {
            final Map<String, Object> contextMap = getVelocityContext(htmlPage, mode, user);
            final StringWriter s = new StringWriter();
            for (String key : contextMap.keySet()) {
                s.append("#set($").append(key).append("=").append(new StringifyObject(contextMap.get(key)).from()).append(')');
            }

            return s.toString();
        } catch (DotDataException | DotSecurityException e) {
            throw new DotRuntimeException(e);
        }
    }

    private static Map<String, Object> getVelocityContext(
            final IHTMLPage htmlPage,
            final PageMode mode,
            final User user) throws DotDataException, DotSecurityException {

        final Map<String, Object> contextMap = populateContext(mode, htmlPage, user);
        final PageContent pageContents = getPageContent(mode, htmlPage);

        for (final Container container : pageContents.getContainers()) {
            final String userContainerId     = getContainerUserId(container);
            contextMap.put("containerIdentifier" + VelocityUtil.escapeContextTokenIdentifier(userContainerId), container.getIdentifier());

            final boolean hasWritePermissionOnContainer = permissionAPI.doesUserHavePermission(container, PERMISSION_WRITE, user, false)
                    && APILocator.getPortletAPI().hasContainerManagerRights(user);
            final boolean hasReadPermissionOnContainer = permissionAPI.doesUserHavePermission(container, PERMISSION_READ, user, false);
            contextMap.put("EDIT_CONTAINER_PERMISSION" + container.getIdentifier(), hasWritePermissionOnContainer);
            if (Config.getBooleanProperty("SIMPLE_PAGE_CONTENT_PERMISSIONING", true)) {
                contextMap.put("USE_CONTAINER_PERMISSION" + container.getIdentifier(), true);
            } else {
                contextMap.put("USE_CONTAINER_PERMISSION" + container.getIdentifier(), hasReadPermissionOnContainer);
            }

            for (final String uniqueId : pageContents.getUUID(container.getIdentifier())) {

                final Collection<Contentlet> pageContentlets = pageContents.getContents(container, uniqueId, new PageRenderContext(mode, user));

                for (final Contentlet contentlet : pageContentlets) {

                    contextMap.put("EDIT_CONTENT_PERMISSION" + contentlet.getIdentifier(),
                            permissionAPI.doesUserHavePermission(contentlet, PERMISSION_WRITE, user));

                    for (final String personalizationToken : pageContents.getPersonalizations(container, uniqueId)) {
                        final String personalization  =  VelocityUtil.escapeContextTokenIdentifier(personalizationToken);
                        final Collection<String> personalizationContentlets = getContentsId(
                                mode, user, pageContents, container, uniqueId, personalizationToken);

                        contextMap.put("contentletList" + container.getIdentifier() + uniqueId + personalization, personalizationContentlets);

                        if (ContainerUUID.UUID_LEGACY_VALUE.equals(uniqueId)) {
                            contextMap.put("contentletList" + container.getIdentifier() + ContainerUUID.UUID_START_VALUE + personalization, personalizationContentlets);
                        } else if (ContainerUUID.UUID_START_VALUE.equals(uniqueId)) {
                            contextMap.put("contentletList" + container.getIdentifier() + ContainerUUID.UUID_LEGACY_VALUE + personalization, personalizationContentlets);
                        }

                    }
                }

                contextMap.put("totalSize" +  container.getIdentifier() + uniqueId, new Integer(pageContentlets.size())); // todo: not sure about this
            }
        }

        return contextMap;
    }

    private static List<String> getContentsId(
            final PageMode mode,
            final User user,
            final PageContent pageContents,
            final Container container,
            final String uniqueId,
            final String personalizationToken) {

        final PageRenderContext pageRenderContext = new PageRenderContext(mode, user);
        return pageContents.getContents(container, uniqueId, personalizationToken, pageRenderContext)
                .stream()
                .map(contentlet2 -> contentlet2.getIdentifier())
                .collect(Collectors.toList());
    }

    private static PageContent getPageContent(
            final PageMode mode,
            final IHTMLPage htmlPage) throws DotDataException, DotSecurityException {

        final HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        final boolean live = isTmDate(request) ?
                        false :
                        mode.showLive;
        return multiTreeAPI.getPageMultiTrees(htmlPage, live);
    }

    private static boolean isTmDate(HttpServletRequest request) {
        return request != null && request.getSession(false) != null && request.getSession().getAttribute("tm_date") != null;
    }

    private static String getContainerUserId (final Container container) {

        return container instanceof FileAssetContainer ? FileAssetContainer.class.cast(container).getPath(): container.getIdentifier();
    }
}
