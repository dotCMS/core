package com.dotcms.rendering.velocity.services;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_CAN_ADD_CHILDREN;
import static com.dotmarketing.business.PermissionAPI.PERMISSION_PUBLISH;
import static com.dotmarketing.business.PermissionAPI.PERMISSION_READ;
import static com.dotmarketing.business.PermissionAPI.PERMISSION_WRITE;

import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.repackage.com.ibm.icu.text.SimpleDateFormat;

import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.MultiTreeAPI;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;

import java.io.StringWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.velocity.context.Context;

import com.google.common.collect.Table;
import com.liferay.portal.model.User;

public class PageContextBuilder {
    private static PermissionAPI permissionAPI = APILocator.getPermissionAPI();

    final IHTMLPage htmlPage;
    final User user;
    final Map<String, Object> ctxMap;
    final PageMode mode;
    final Date timeMachine;
    List<Tag> pageFoundTags;

    final static String WIDGET_PRE_EXECUTE = "WIDGET_PRE_EXECUTE";
    public PageContextBuilder(IHTMLPage htmlPage, User user, PageMode mode, Date timeMachine)
            throws DotSecurityException, DotDataException {
        super();
        this.htmlPage = htmlPage;
        this.user = user;
        this.ctxMap = new HashMap<>();
        this.mode = mode;
        this.timeMachine = timeMachine;
        populateContext();
        populateContainers();
    }



    public PageContextBuilder(IHTMLPage htmlPage, User user, PageMode mode) throws DotSecurityException, DotDataException {
        this(htmlPage, user, mode, null);
    }

    private void populateContext() throws DotDataException, DotSecurityException {


            ctxMap.put(mode.name(), Boolean.TRUE);
        
        
        
        
        
        
        // set the page cache var
        if (htmlPage.getCacheTTL() > 0 && LicenseUtil.getLevel() >= LicenseLevel.COMMUNITY.level) {
            ctxMap.put("dotPageCacheDate", new java.util.Date());
            ctxMap.put("dotPageCacheTTL", htmlPage.getCacheTTL());
        }

        String templateId = htmlPage.getTemplateId();
        Template template = (mode.showLive) ? (Template) APILocator.getVersionableAPI()
            .findLiveVersion(templateId, user, false)
                : (Template) APILocator.getVersionableAPI()
                    .findWorkingVersion(templateId, user, false);

        Identifier pageIdent = APILocator.getIdentifierAPI()
            .find(htmlPage.getIdentifier());

        // gets pageChannel for this path
        java.util.StringTokenizer st = new java.util.StringTokenizer(String.valueOf(pageIdent.getURI()), "/");
        String pageChannel = null;
        if (st.hasMoreTokens()) {
            pageChannel = st.nextToken();
        }

        // to check user has permission to write on this page
        List<PublishingEndPoint> receivingEndpoints = APILocator.getPublisherEndPointAPI()
            .getReceivingEndPoints();
        boolean hasAddChildrenPermOverHTMLPage =
                permissionAPI.doesUserHavePermission(htmlPage, PERMISSION_CAN_ADD_CHILDREN, user);
        boolean hasWritePermOverHTMLPage = permissionAPI.doesUserHavePermission(htmlPage, PERMISSION_WRITE, user);
        boolean hasPublishPermOverHTMLPage = permissionAPI.doesUserHavePermission(htmlPage, PERMISSION_PUBLISH, user);
        boolean hasRemotePublishPermOverHTMLPage =
                hasPublishPermOverHTMLPage && LicenseUtil.getLevel() >= LicenseLevel.STANDARD.level;
        boolean hasEndPoints = UtilMethods.isSet(receivingEndpoints) && !receivingEndpoints.isEmpty();
        boolean canUserWriteOnTemplate =
                permissionAPI.doesUserHavePermission(template, PERMISSION_WRITE, user) && APILocator.getPortletAPI()
                    .hasTemplateManagerRights(user);

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
        ctxMap.put("TEMPLATE_INODE", template.getIdentifier());
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



    }



    private void populateContainers() throws DotDataException, DotSecurityException {
        Table<String, String, Set<String>> pageContents = new MultiTreeAPI().getPageMultiTrees(htmlPage, mode.showLive);



        if (!pageContents.isEmpty()) {
            for (final String containerId : pageContents.rowKeySet()) {
                for (final String uniqueId : pageContents.row(containerId)
                    .keySet()) {
                    Set<String> cons = pageContents.get(containerId, uniqueId);

                    Container c = (mode.showLive) ? (Container) APILocator.getVersionableAPI()
                        .findLiveVersion(containerId, user, false)
                            : (Container) APILocator.getVersionableAPI()
                                .findWorkingVersion(containerId, user, false);

                    boolean hasWritePermissionOnContainer =
                            permissionAPI.doesUserHavePermission(c, PERMISSION_WRITE, user, false) && APILocator.getPortletAPI()
                                .hasContainerManagerRights(user);
                    boolean hasReadPermissionOnContainer = permissionAPI.doesUserHavePermission(c, PERMISSION_READ, user, false);
                    ctxMap.put("EDIT_CONTAINER_PERMISSION" + c.getIdentifier(), hasWritePermissionOnContainer);
                    if (Config.getBooleanProperty("SIMPLE_PAGE_CONTENT_PERMISSIONING", true))
                        ctxMap.put("USE_CONTAINER_PERMISSION" + c.getIdentifier(), true);
                    else
                        ctxMap.put("USE_CONTAINER_PERMISSION" + c.getIdentifier(), hasReadPermissionOnContainer);

                    // to check user has permission to write this container
                    boolean hasWritePermOverTheStructure = false;

                    for (ContainerStructure cs : APILocator.getContainerAPI()
                        .getContainerStructures(c)) {
                        Structure st = CacheLocator.getContentTypeCache()
                            .getStructureByInode(cs.getStructureId());

                        hasWritePermOverTheStructure |= permissionAPI.doesUserHavePermission(st, PERMISSION_WRITE, user);
                    }

                    ctxMap.put("ADD_CONTENT_PERMISSION" + c.getIdentifier(), new Boolean(hasWritePermOverTheStructure));

                    List<Contentlet> contentlets = APILocator.getContentletAPI()
                        .findContentletsByIdentifiers(cons.stream()
                            .toArray(String[]::new), mode.showLive, -1, user, false);

                    // get contentlets only for main frame

                    if (contentlets != null) {
                        for (Contentlet contentlet : contentlets) {
                            ctxMap.put("EDIT_CONTENT_PERMISSION" + contentlet.getIdentifier(),
                                    permissionAPI.doesUserHavePermission(contentlet, PERMISSION_WRITE, user));
                            ContentType type = contentlet.getContentType();
                            if (type.baseType() == BaseContentType.WIDGET) {
                                com.dotcms.contenttype.model.field.Field field = type.fieldMap()
                                    .get("widgetPreexecute");
                                if (field != null && UtilMethods.isSet(field.values())) {

                                    String x = ctxMap.containsKey(WIDGET_PRE_EXECUTE) ? ""
                                            : (String) ctxMap.get(WIDGET_PRE_EXECUTE);
                                    ctxMap.put(WIDGET_PRE_EXECUTE, x + field.values() + "\n");
                                }
                            }

                            // Check if we want to accrue the tags associated to each contentlet on
                            // this page
                            if (Config.getBooleanProperty("ACCRUE_TAGS_IN_CONTENTS_ON_PAGE", false)) {


                                // Search for the tags associated to this contentlet inode
                                List<Tag> contentletFoundTags = APILocator.getTagAPI()
                                    .getTagsByInode(contentlet.getInode());
                                if (contentletFoundTags != null) {
                                    this.pageFoundTags.addAll(contentletFoundTags);
                                }

                            }

                        }
                    }
                    // sets contentletlist with all the files to load per
                    // container
                    ctxMap.put("contentletList" + c.getIdentifier() + uniqueId, contentlets.stream()
                        .map(con -> con.getIdentifier())
                        .toArray(size -> new String[size]));
                    ctxMap.put("totalSize" + c.getIdentifier() + uniqueId, new Integer(contentlets.size()));

                }
            }
        }
    }



    public List<Tag> getPageFoundTags() {
        return pageFoundTags;
    }

    public String getWidgetPreExecute() {
        return (String) ctxMap.getOrDefault(WIDGET_PRE_EXECUTE, null);
    }




    public Context addAll(Context incoming) {

        for (String key : this.ctxMap.keySet()) {
            incoming.put(key, ctxMap.get(key));
        }



        return incoming;
    }



    public String asString() {


        final StringWriter s = new StringWriter();
        for (String key : this.ctxMap.keySet()) {
            s.append("#set($")
                .append(key)
                .append("=")
                .append(new StringifyObject(ctxMap.get(key)).from())
                .append(')');
        }


        return s.toString();

    }



}
