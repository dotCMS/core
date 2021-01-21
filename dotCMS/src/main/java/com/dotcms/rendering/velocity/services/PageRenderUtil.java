package com.dotcms.rendering.velocity.services;


import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.rendering.velocity.directive.ParseContainer;
import com.dotcms.rendering.velocity.viewtools.DotTemplateTool;
import com.dotcms.repackage.com.google.common.collect.Lists;
import com.dotcms.visitor.domain.Visitor;
import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.MultiTreeAPI;
import com.dotmarketing.factories.PersonalizedContentlet;
import com.dotmarketing.portlets.containers.business.*;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.transform.DotContentletTransformer;
import com.dotmarketing.portlets.contentlet.transform.DotTransformerBuilder;
import com.dotmarketing.portlets.htmlpageasset.business.render.ContainerRaw;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.personas.business.PersonaAPI;
import com.dotmarketing.portlets.personas.model.IPersona;
import com.dotmarketing.portlets.personas.model.Persona;
import com.dotmarketing.portlets.templates.design.bean.ContainerUUID;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.tag.business.TagAPI;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.util.*;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import com.rainerhahnekamp.sneakythrow.Sneaky;
import org.apache.velocity.context.Context;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Supplier;

import static com.dotmarketing.business.PermissionAPI.*;

/**
 * Util class for rendering a Page
 */
public class PageRenderUtil implements Serializable {

    public static String CONTAINER_UUID_PREFIX = "uuid-";
    private static final long serialVersionUID = 1L;

    private final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
    private final MultiTreeAPI multiTreeAPI = APILocator.getMultiTreeAPI();
    private final ContentletAPI contentletAPI = APILocator.getContentletAPI();
    private final TagAPI tagAPI = APILocator.getTagAPI();
    private final PersonaAPI personaAPI = APILocator.getPersonaAPI();

    final IHTMLPage htmlPage;
    final User user;
    final Map<String, Object> contextMap; // this is the velocity runtime context
    final PageMode mode;
    final List<Tag> pageFoundTags;
    final StringBuilder widgetPreExecute;
    final static String WIDGET_PRE_EXECUTE = "WIDGET_PRE_EXECUTE";
    final List<ContainerRaw> containersRaw;
    final long languageId;
    final Host site;
    final TemplateLayout templateLayout;


    public PageRenderUtil(
            final IHTMLPage htmlPage,
            final User user,
            final PageMode mode,
            final long languageId,
            final Host site) throws DotSecurityException, DotDataException {

        if (site == null) {
            this.site = APILocator.getHostAPI().findDefaultHost(user, mode.respectAnonPerms);
        } else {
            this.site = site;
        }

        this.pageFoundTags = Lists.newArrayList();
        this.htmlPage = htmlPage;
        this.user = user;
        this.mode = mode;
        this.languageId = languageId;
        this.widgetPreExecute = new StringBuilder();
        final Template template = APILocator.getHTMLPageAssetAPI().getTemplate(htmlPage, !mode.showLive);
        this.templateLayout = template != null && template.isDrawed() ? DotTemplateTool.themeLayout(template.getInode()) : null;
        this.contextMap = populateContext();
        this.containersRaw = populateContainers();
    }

    public PageRenderUtil(final HTMLPageAsset htmlPage, final User user, final PageMode mode)
            throws DotSecurityException, DotDataException {
        this(htmlPage, user, mode, htmlPage.getLanguageId(), APILocator.getHostAPI().find(htmlPage.getHost(), user, false));
    }

    private Map<String, Object> populateContext() throws DotDataException, DotSecurityException {
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

    private Container getLiveContainerById(final String containerId) throws DotSecurityException, DotDataException {

        final LiveContainerFinderByIdOrPathStrategyResolver strategyResolver =
                LiveContainerFinderByIdOrPathStrategyResolver.getInstance();
        final Optional<ContainerFinderByIdOrPathStrategy> strategy = strategyResolver.get(containerId);
        final ContainerFinderByIdOrPathStrategy liveStrategy = strategy.isPresent() ? strategy.get() : strategyResolver.getDefaultStrategy();
        final Supplier<Host> resourceHostSupplier = () -> this.site;


        Container container = null;
        try {

            container =
                    liveStrategy.apply(containerId, APILocator.systemUser(), false, resourceHostSupplier);
        } catch (NotFoundInDbException e) {

            container = null;
        }

        return container;
    }

    private Container getLiveContainerById(final String containerIdOrPath, final User user, final Template template) throws NotFoundInDbException {

        final LiveContainerFinderByIdOrPathStrategyResolver strategyResolver =
                LiveContainerFinderByIdOrPathStrategyResolver.getInstance();
        final Optional<ContainerFinderByIdOrPathStrategy> strategy = strategyResolver.get(containerIdOrPath);

        return this.geContainerById(containerIdOrPath, user, template, strategy, strategyResolver.getDefaultStrategy());
    }

    private Container getWorkingContainerById(final String containerIdOrPath, final User user, final Template template) throws NotFoundInDbException {

        final WorkingContainerFinderByIdOrPathStrategyResolver strategyResolver =
                WorkingContainerFinderByIdOrPathStrategyResolver.getInstance();
        final Optional<ContainerFinderByIdOrPathStrategy> strategy = strategyResolver.get(containerIdOrPath);

        return this.geContainerById(containerIdOrPath, user, template, strategy, strategyResolver.getDefaultStrategy());
    }

    private Container geContainerById(final String containerIdOrPath, final User user, final Template template,
                                      final Optional<ContainerFinderByIdOrPathStrategy> strategy,
                                      final ContainerFinderByIdOrPathStrategy defaultContainerFinderByIdOrPathStrategy) throws NotFoundInDbException {

        final Supplier<Host> resourceHostSupplier = Sneaky.sneaked(() -> APILocator.getTemplateAPI().getTemplateHost(template));

        return strategy.isPresent() ?
                strategy.get().apply(containerIdOrPath, user, false, resourceHostSupplier) :
                defaultContainerFinderByIdOrPathStrategy.apply(containerIdOrPath, user, false, resourceHostSupplier);
    }

    private List<ContainerRaw> populateContainers() throws DotDataException, DotSecurityException {

        final HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        final boolean live               = this.isLive(request);
        final Table<String, String, Set<PersonalizedContentlet>> pageContents = this.multiTreeAPI.getPageMultiTrees(htmlPage, live);
        final Set<String> personalizationsForPage = this.multiTreeAPI.getPersonalizationsForPage(htmlPage);
        final List<ContainerRaw> raws  = Lists.newArrayList();
        final String includeContentFor = this.getPersonaTagToIncludeContent(request, personalizationsForPage);

        for (final String containerId : pageContents.rowKeySet()) {

            final Container container = this.getContainer(live, containerId);

            if (container == null) {
                continue;
            }

            final List<ContainerStructure> containerStructures = APILocator.getContainerAPI().getContainerStructures(container);
            this.addPermissions(container);

            final Map<String, List<Contentlet>> contentMaps = Maps.newLinkedHashMap();
            final ContainerUUIDPersona containerUuidPersona = new ContainerUUIDPersona();

            for (final String uniqueId : pageContents.row(containerId).keySet()) {

                final String uniqueUUIDForRender = needParseContainerPrefix(container, uniqueId) ?
                        ParseContainer.getDotParserContainerUUID(uniqueId) : uniqueId;

                if (ContainerUUID.UUID_DEFAULT_VALUE.equals(uniqueId)) {
                    continue;
                }

                final Collection<PersonalizedContentlet> personalizedContentletSet = pageContents.get(containerId, uniqueId);
                final List<Contentlet> personalizedContentletMap          = Lists.newArrayList();

                for (final PersonalizedContentlet personalizedContentlet : personalizedContentletSet) {

                    final Contentlet nonHydratedContentlet = this.getContentlet(personalizedContentlet);

                    if (nonHydratedContentlet == null) {
                        continue;
                    }

                    final DotContentletTransformer transformer = new DotTransformerBuilder()
                            .defaultOptions().content(nonHydratedContentlet).build();
                    final Contentlet contentlet = transformer.hydrate().get(0);

                    final long contentsSize = containerUuidPersona
                            .getSize(container, uniqueUUIDForRender, personalizedContentlet);

                    if (container.getMaxContentlets() < contentsSize) {

                        Logger.debug(this, ()-> "Contentlet: "          + contentlet.getIdentifier()
                                + ", has been skipped. Max contentlet: "    + container.getMaxContentlets()
                                + ", has been overcome for the container: " + containerId);
                        continue;
                    }

                    containerUuidPersona.add(container, uniqueUUIDForRender, personalizedContentlet);


                    contextMap.put("EDIT_CONTENT_PERMISSION" + contentlet.getIdentifier(),
                            permissionAPI.doesUserHavePermission(contentlet, PERMISSION_WRITE, user));

                    this.widgetPreExecute(contentlet);
                    this.addAccrueTags(contentlet);

                    if (personalizedContentlet.getPersonalization().equals(includeContentFor)) {

                        contentlet.getMap().put("contentType", contentlet.getContentType().variable());
                        personalizedContentletMap.add(contentlet);
                    }
                }

                contentMaps.put(CONTAINER_UUID_PREFIX + uniqueUUIDForRender, personalizedContentletMap);
            }

            for (final Map.Entry<String, List<String>> entry : containerUuidPersona.entrySet()) {

                contextMap.put("contentletList" + entry.getKey(), entry.getValue());
                contextMap.put("totalSize"      + entry.getKey(), entry.getValue().size());
            }

            raws.add(new ContainerRaw(container, containerStructures, contentMaps));
        }

        return raws;
    }

    private boolean isLive(final HttpServletRequest request) {

        return request != null && request.getSession(false) != null && request.getSession(false).getAttribute("tm_date") != null ?
                false :
                mode.showLive;
    }

    private Container getContainer(final boolean live, final String containerId) throws DotSecurityException, DotDataException {
        final Optional<Container> optionalContainer =
                APILocator.getContainerAPI().findContainer(containerId, APILocator.systemUser(), live, false);
        return optionalContainer.isPresent() ? optionalContainer.get() : null;
    }

    private void addPermissions(final Container container) throws DotDataException {

        final boolean hasWritePermissionOnContainer = permissionAPI.doesUserHavePermission(container, PERMISSION_WRITE, user, false)
                && APILocator.getPortletAPI().hasContainerManagerRights(user);
        final boolean hasReadPermissionOnContainer  = permissionAPI.doesUserHavePermission(container, PERMISSION_READ,  user, false);
        contextMap.put("EDIT_CONTAINER_PERMISSION" + container.getIdentifier(), hasWritePermissionOnContainer);

        if (Config.getBooleanProperty("SIMPLE_PAGE_CONTENT_PERMISSIONING", true)) {

            contextMap.put("USE_CONTAINER_PERMISSION" + container.getIdentifier(), true);
        } else {

            contextMap.put("USE_CONTAINER_PERMISSION" + container.getIdentifier(), hasReadPermissionOnContainer);
        }
    }

    /* Check if we want to accrue the tags associated to each contentlet on
     * this page
     */
    private void addAccrueTags(final Contentlet contentlet) throws DotDataException {

        if (Config.getBooleanProperty("ACCRUE_TAGS_IN_CONTENTS_ON_PAGE", false)) {

            // Search for the tags associated to this contentlet inode
            final List<Tag> contentletFoundTags = this.tagAPI.getTagsByInode(contentlet.getInode());
            if (contentletFoundTags != null) {
                this.pageFoundTags.addAll(contentletFoundTags);
            }
        }
    }

    private void widgetPreExecute(final Contentlet contentlet) {

        final ContentType type = contentlet.getContentType();
        if (type.baseType() == BaseContentType.WIDGET) {

            final com.dotcms.contenttype.model.field.Field field = type.fieldMap().get("widgetPreexecute");
            if (field != null && UtilMethods.isSet(field.values())) {

                widgetPreExecute.append(field.values());
            }
        }
    }

    private boolean needParseContainerPrefix(final Container container, final String uniqueId) {
        return !ParseContainer.isParserContainerUUID(uniqueId) &&
                (templateLayout == null || !templateLayout.existsContainer(container, uniqueId));
    }

    private Contentlet getContentlet(final PersonalizedContentlet personalizedContentlet) {

        return Config.getBooleanProperty("DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE", false)?
                getContentletOrFallback(personalizedContentlet) : getSpecificContentlet(personalizedContentlet);
    }

    private Contentlet getSpecificContentlet(final PersonalizedContentlet personalizedContentlet) {
        try {

            final Contentlet contentlet = contentletAPI.findContentletByIdentifier
                    (personalizedContentlet.getContentletId(), mode.showLive, this.resolveLanguageId(), user, mode.respectAnonPerms);

            return contentlet;
        } catch (final DotContentletStateException e) {
            // Expected behavior, DotContentletState Exception is used for flow control
            return null;
        } catch (Exception e) {
            throw new DotStateException(e);
        }
    }

    private long resolveLanguageId () {

        final HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        if (null != request) {

            final Language currentLanguage =
                    WebAPILocator.getLanguageWebAPI().getLanguage(request);
            if (null != currentLanguage) {

                return currentLanguage.getId();
            }
        }

        return this.languageId;
    }

    private Contentlet getContentletOrFallback(final PersonalizedContentlet personalizedContentlet) {
        try {

            final Optional<Contentlet> contentletOpt = contentletAPI.findContentletByIdentifierOrFallback
                    (personalizedContentlet.getContentletId(), mode.showLive, languageId, user, mode.respectAnonPerms);

            final Contentlet contentlet = contentletOpt.isPresent()
                    ? contentletOpt.get() : contentletAPI.findContentletByIdentifierAnyLanguage(personalizedContentlet.getContentletId());

            return contentlet;
        } catch (final DotContentletStateException e) {
            // Expected behavior, DotContentletState Exception is used for flow control
            return null;
        } catch (Exception e) {
            throw new DotStateException(e);
        }
    }


    public List<Tag> getPageFoundTags() {
        return this.pageFoundTags;
    }


    final String getWidgetPreExecute() {
        return this.widgetPreExecute.toString();
    }


    public Context addAll(Context incoming) {

        for (String key : this.contextMap.keySet()) {
            incoming.put(key, this.contextMap.get(key));
        }

        return incoming;
    }

    /**
     * Return the Velocity code to set the Page's variables as: contentletList, contentType,
     * totalSize, etc.
     *
     * @return
     */
    public String asString() {

        final StringWriter s = new StringWriter();
        for (String key : this.contextMap.keySet()) {
            s.append("#set($").append(key.replace(":", "")).append("=").append(new StringifyObject(this.contextMap.get(key)).from()).append(')');
        }

        return s.toString();

    }

    /**
     * Return the Page's {@link ContainerRaw}
     *
     * @return
     */
    public List<ContainerRaw> getContainersRaw() {
        return this.containersRaw;
    }

    private String getPersonaTagToIncludeContent(final HttpServletRequest request, final Set<String> personalizationsForPage) {
        IPersona iPersona = null;

        if (request != null) {
            final Optional<Visitor> visitor = APILocator.getVisitorAPI().getVisitor(request);
            iPersona = visitor.isPresent() && visitor.get().getPersona() != null ? visitor.get().getPersona() : null;
        }

        final String currentPersonaTag = iPersona == null ? MultiTree.DOT_PERSONALIZATION_DEFAULT
                : Persona.DOT_PERSONA_PREFIX_SCHEME + StringPool.COLON + iPersona.getKeyTag();

        final boolean hasPersonalizations = personalizationsForPage.contains(currentPersonaTag);

        return hasPersonalizations ? currentPersonaTag : MultiTree.DOT_PERSONALIZATION_DEFAULT;
    }

    /**
     * Util class to sort the {@link Contentlet} by {@link Persona} and {@link Container}
     */
    private static class ContainerUUIDPersona {
        Map<String, List<String>> contents = Maps.newHashMap();

        public void add(
                final Container container,
                final String uniqueUUIDForRender,
                final PersonalizedContentlet personalizedContentlet) {

            get(container, uniqueUUIDForRender, personalizedContentlet)
                    .add(personalizedContentlet.getContentletId());
        }

        private List<String> get(Container container, String uniqueUUIDForRender, PersonalizedContentlet personalizedContentlet) {
            return contents
                    .computeIfAbsent(
                            getKey(container, uniqueUUIDForRender, personalizedContentlet),
                            k -> Lists.newArrayList()
                    );
        }

        private String getKey(Container container, String uniqueUUIDForRender, PersonalizedContentlet personalizedContentlet) {
            return container.getIdentifier() + uniqueUUIDForRender + personalizedContentlet.getPersonalization();
        }

        public long getSize(
                final Container container,
                final String uniqueUUIDForRender,
                final PersonalizedContentlet personalizedContentlet) {
            return get(container, uniqueUUIDForRender, personalizedContentlet).size();
        }

        public Set<? extends Map.Entry<String, List<String>>> entrySet() {
            return contents.entrySet();
        }
    }
}
