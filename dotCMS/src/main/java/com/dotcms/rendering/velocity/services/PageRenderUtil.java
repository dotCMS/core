package com.dotcms.rendering.velocity.services;


import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.repackage.com.google.common.collect.Lists;
import com.dotcms.repackage.com.ibm.icu.text.SimpleDateFormat;
import com.dotcms.util.CollectionsUtils;
import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.MultiTreeAPI;
import com.dotmarketing.factories.PersonalizedContentlet;
import com.dotmarketing.portlets.containers.business.ContainerExceptionNotifier;
import com.dotmarketing.portlets.containers.business.ContainerFinderByIdOrPathStrategy;
import com.dotmarketing.portlets.containers.business.LiveContainerFinderByIdOrPathStrategyResolver;
import com.dotmarketing.portlets.containers.business.WorkingContainerFinderByIdOrPathStrategyResolver;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.containers.model.FileAssetContainer;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.util.ContentletUtil;
import com.dotmarketing.portlets.htmlpageasset.business.render.ContainerRaw;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.templates.design.bean.ContainerUUID;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.tag.business.TagAPI;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.VelocityUtil;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import com.rainerhahnekamp.sneakythrow.Sneaky;
import org.apache.velocity.context.Context;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.dotmarketing.business.PermissionAPI.*;

/**
 * Util class for rendering a Page
 */
public class PageRenderUtil implements Serializable {

    private static final long serialVersionUID = 1L;

    private final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
    private final MultiTreeAPI  multiTreeAPI  = APILocator.getMultiTreeAPI();
    private final ContentletAPI contentletAPI = APILocator.getContentletAPI();
    private final TagAPI        tagAPI        = APILocator.getTagAPI();

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


    public PageRenderUtil(
            final IHTMLPage htmlPage,
            final User user,
            final PageMode mode,
            final long languageId,
            final Host site) throws DotSecurityException, DotDataException {

        this.pageFoundTags = Lists.newArrayList();
        this.htmlPage = htmlPage;
        this.user = user;
        this.mode = mode;
        this.languageId=languageId;
        this.widgetPreExecute = new StringBuilder();
        this.contextMap = populateContext();
        this.containersRaw = populateContainers();
        this.site = null == site? APILocator.getHostAPI().findDefaultHost(user, mode.respectAnonPerms):site;

    }

    public PageRenderUtil(final IHTMLPage htmlPage, final User user, final PageMode mode, final long languageId)
            throws DotSecurityException, DotDataException {
        this(htmlPage, user, mode, languageId, null);

    }

    public PageRenderUtil(final IHTMLPage htmlPage, final User user, final PageMode mode)
            throws DotSecurityException, DotDataException {
        this(htmlPage,user, mode, htmlPage.getLanguageId(), null);
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

    private Container getLiveContainerById (final String containerId) throws DotSecurityException, DotDataException {

        final LiveContainerFinderByIdOrPathStrategyResolver strategyResolver =
                LiveContainerFinderByIdOrPathStrategyResolver.getInstance();
        final Optional<ContainerFinderByIdOrPathStrategy> strategy = strategyResolver.get(containerId);
        final ContainerFinderByIdOrPathStrategy liveStrategy    = strategy.isPresent()?strategy.get():strategyResolver.getDefaultStrategy();
        final Supplier<Host>                  resourceHostSupplier = ()-> this.site;


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
        final Optional<ContainerFinderByIdOrPathStrategy> strategy           = strategyResolver.get(containerIdOrPath);

        return this.geContainerById(containerIdOrPath, user, template, strategy, strategyResolver.getDefaultStrategy());
    }

    private Container getWorkingContainerById(final String containerIdOrPath, final User user, final Template template) throws NotFoundInDbException {

        final WorkingContainerFinderByIdOrPathStrategyResolver strategyResolver =
                WorkingContainerFinderByIdOrPathStrategyResolver.getInstance();
        final Optional<ContainerFinderByIdOrPathStrategy> strategy           = strategyResolver.get(containerIdOrPath);

        return this.geContainerById(containerIdOrPath, user, template, strategy, strategyResolver.getDefaultStrategy());
    }

    private Container geContainerById(final String containerIdOrPath, final User user, final Template template,
                                      final Optional<ContainerFinderByIdOrPathStrategy> strategy,
                                      final ContainerFinderByIdOrPathStrategy defaultContainerFinderByIdOrPathStrategy) throws NotFoundInDbException  {

        final Supplier<Host> resourceHostSupplier                            =  Sneaky.sneaked(()->APILocator.getTemplateAPI().getTemplateHost(template));

        return strategy.isPresent()?
                strategy.get().apply(containerIdOrPath, user, false, resourceHostSupplier):
                defaultContainerFinderByIdOrPathStrategy.apply(containerIdOrPath, user, false, resourceHostSupplier);
    }

    private List<ContainerRaw> populateContainers() throws DotDataException, DotSecurityException {

        final HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        final boolean live =
                request != null && request.getSession(false) != null && request.getSession().getAttribute("tm_date") != null ?
                        false :
                        mode.showLive;
        final Table<String, String, Set<PersonalizedContentlet>> pageContents = this.multiTreeAPI.getPageMultiTrees(htmlPage, live);
        final List<ContainerRaw> raws = Lists.newArrayList();

        for (final String containerId : pageContents.rowKeySet()) {

            Container container = null;
            final WorkingContainerFinderByIdOrPathStrategyResolver strategyResolver =
                    WorkingContainerFinderByIdOrPathStrategyResolver.getInstance();
            final Optional<ContainerFinderByIdOrPathStrategy> strategy = strategyResolver.get(containerId);
            final ContainerFinderByIdOrPathStrategy workingStrategy    = strategy.isPresent()?strategy.get():strategyResolver.getDefaultStrategy();
            final Supplier<Host>                  resourceHostSupplier = ()-> this.site;

            try {
                if (live) {

                    container = this.getLiveContainerById(containerId);
                    if (null == container) {
                        container = workingStrategy.apply(containerId, APILocator.systemUser(), false, resourceHostSupplier);
                    }
                } else {
                    container = workingStrategy.apply(containerId, APILocator.systemUser(), false, resourceHostSupplier);
                }
            } catch (NotFoundInDbException | DotRuntimeException e) {

                new ContainerExceptionNotifier(e, containerId).notifyUser();
                container = null;
            }

            if (container == null) {
                continue;
            }
            
            final List<ContainerStructure> containerStructures = APILocator.getContainerAPI().getContainerStructures(container);
            final boolean hasWritePermissionOnContainer = permissionAPI.doesUserHavePermission(container, PERMISSION_WRITE, user, false)
                    && APILocator.getPortletAPI().hasContainerManagerRights(user);
            final boolean hasReadPermissionOnContainer = permissionAPI.doesUserHavePermission(container, PERMISSION_READ, user, false);
            contextMap.put("EDIT_CONTAINER_PERMISSION" + container.getIdentifier(), hasWritePermissionOnContainer);
            if (Config.getBooleanProperty("SIMPLE_PAGE_CONTENT_PERMISSIONING", true)) {
                contextMap.put("USE_CONTAINER_PERMISSION" + container.getIdentifier(), true);
            } else {
                contextMap.put("USE_CONTAINER_PERMISSION" + container.getIdentifier(), hasReadPermissionOnContainer);
            }


            final Map<String, List<Map<String,Object>>> contentMaps = Maps.newLinkedHashMap();
            
            for (final String uniqueId : pageContents.row(containerId).keySet()) {

                if(ContainerUUID.UUID_DEFAULT_VALUE.equals(uniqueId)) {
                    continue;
                }

                final Map<String, List<String>> contentIdListByPersonalizationMap = new HashMap<>();
                final Set<PersonalizedContentlet> personalizedContentletSet = pageContents.get(containerId, uniqueId);
                final List<Contentlet> contentlets = personalizedContentletSet.stream().map(personalizedContentlet -> {
                    try {

                        // indexing
                        final String contentletIdentifier = personalizedContentlet.getContentletId();
                        final String personalization      = personalizedContentlet.getPersonalization();

                        final Optional<Contentlet> contentletOpt = contentletAPI.findContentletByIdentifierOrFallback
                                (personalizedContentlet.getContentletId(), mode.showLive, languageId, user, mode.respectAnonPerms);

                        final Contentlet contentlet =  contentletOpt.isPresent()
                                ? contentletOpt.get() : contentletAPI.findContentletByIdentifierAnyLanguage(personalizedContentlet.getContentletId());

                        CollectionsUtils.computeSubValueIfAbsent(
                                contentIdListByPersonalizationMap, personalization, contentletIdentifier,
                                CollectionsUtils::add, (key, identifier)-> CollectionsUtils.list(contentletIdentifier));

                        return contentlet;
                    } catch (final DotContentletStateException e) {
                        // Expected behavior, DotContentletState Exception is used for flow control
                        return null;
                    } catch (Exception e) {
                        throw new DotStateException(e);
                    }
                }).filter(Objects::nonNull).collect(Collectors.toList());


                final List<Map<String, Object>> cListAsMaps = Lists.newArrayList();
                for (final Contentlet contentlet : contentlets) {

                    try {
                        final Map<String,Object> contentPrintableMap = ContentletUtil.getContentPrintableMap(user, contentlet);
                        contentPrintableMap.put("contentType", contentlet.getContentType().variable());
                        // contentPrintableMap.put("personalization", personalization); // todo: not sure if this will be needed by FOX
                        cListAsMaps.add(contentPrintableMap);
                    } catch (IOException e) {
                        throw new DotStateException(e);
                    }

                    contextMap.put("EDIT_CONTENT_PERMISSION" + contentlet.getIdentifier(),
                            permissionAPI.doesUserHavePermission(contentlet, PERMISSION_WRITE, user));
                    final ContentType type = contentlet.getContentType();
                    if (type.baseType() == BaseContentType.WIDGET) {
                        final com.dotcms.contenttype.model.field.Field field = type.fieldMap().get("widgetPreexecute");
                        if (field != null && UtilMethods.isSet(field.values())) {
                            widgetPreExecute.append(field.values());
                        }
                    }

                    // Check if we want to accrue the tags associated to each contentlet on
                    // this page
                    if (Config.getBooleanProperty("ACCRUE_TAGS_IN_CONTENTS_ON_PAGE", false)) {

                        // Search for the tags associated to this contentlet inode
                        final List<Tag> contentletFoundTags = this.tagAPI.getTagsByInode(contentlet.getInode());
                        if (contentletFoundTags != null) {
                            this.pageFoundTags.addAll(contentletFoundTags);
                        }

                    }
                }
                
                contentMaps.put((uniqueId.startsWith("uuid-")) ? uniqueId : "uuid-" + uniqueId, cListAsMaps);
                this.setContentletListPerPersonalization(uniqueId, container, contentIdListByPersonalizationMap);
                contextMap.put("totalSize" +  container.getIdentifier() + uniqueId, new Integer(contentlets.size())); // todo: not sure about this
            }

            raws.add(new ContainerRaw(container, containerStructures, contentMaps));
        }
        
        return raws;
    }

    private void setContentletListPerPersonalization (final String uniqueId, final Container container,
                                    final Map<String, List<String>> contentIdListByPersonalizationMap) {

        final String containerIdentifier = container.getIdentifier();
        final String userContainerId     = getContainerUserId(container);
        contextMap.put("containerIdentifier" + VelocityUtil.escapeContextTokenIdentifier(userContainerId), containerIdentifier);

        for (final String personalizationToken : contentIdListByPersonalizationMap.keySet()) {

            final String personalization  =  VelocityUtil.escapeContextTokenIdentifier(personalizationToken);
            final String[] contentStrList = contentIdListByPersonalizationMap.get(personalizationToken).toArray(new String[0]);
            contextMap.put("contentletList" + containerIdentifier + uniqueId + personalization, contentStrList);

            if (ContainerUUID.UUID_LEGACY_VALUE.equals(uniqueId)) { // todo: not sure about them
                contextMap.put("contentletList" + containerIdentifier + ContainerUUID.UUID_START_VALUE + personalization, contentStrList);
            } else if (ContainerUUID.UUID_START_VALUE.equals(uniqueId)) {
                contextMap.put("contentletList" + containerIdentifier + ContainerUUID.UUID_LEGACY_VALUE + personalization, contentStrList);
            }
        }

        // todo: test if remove or not this fallback
        final String currentPersonalization = WebAPILocator.getPersonalizationWebAPI().getContainerPersonalization();
        if (contentIdListByPersonalizationMap.containsKey(currentPersonalization)) {
            final String[] contentStrList = contentIdListByPersonalizationMap.get(currentPersonalization).toArray(new String[0]);
            contextMap.put("contentletList" + containerIdentifier + uniqueId, contentStrList);

            if (ContainerUUID.UUID_LEGACY_VALUE.equals(uniqueId)) {
                contextMap.put("contentletList" + containerIdentifier + ContainerUUID.UUID_START_VALUE, contentStrList);
            } else if (ContainerUUID.UUID_START_VALUE.equals(uniqueId)) {
                contextMap.put("contentletList" + containerIdentifier + ContainerUUID.UUID_LEGACY_VALUE, contentStrList);
            }
        }
    } // setContentletListPerPersonalization.

    private String getContainerUserId (final Container container) {

        return container instanceof FileAssetContainer? FileAssetContainer.class.cast(container).getPath(): container.getIdentifier();
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
            s.append("#set($").append(key).append("=").append(new StringifyObject(this.contextMap.get(key)).from()).append(')');
        }

        return s.toString();

    }

    /**
     * Return the Page's {@link ContainerRaw}
     * @return
     */
    public List<ContainerRaw> getContainersRaw() {
        return this.containersRaw;
    }


}
