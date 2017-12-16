package com.dotcms.rendering.velocity.services;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.rendering.velocity.DotResourceCache;
import com.dotcms.rendering.velocity.VelocityType;
import com.dotcms.repackage.bsh.This;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.VersionableAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.MultiTreeAPI;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.TagUtil;
import com.dotmarketing.util.UtilMethods;
import com.dotcms.rendering.velocity.util.VelocityUtil;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.resource.ResourceManager;

import com.google.common.collect.Table;
import com.liferay.portal.model.User;

/**
 * @author will
 *
 *         To change this generated comment edit the template variable "typecomment":
 *         Window>Preferences>Java>Templates. To enable and disable the creation of type comments go
 *         to Window>Preferences>Java>Code Generation.
 */
public class PageServices implements VelocityCMSObject {

    /**
     * Invalidates live and working html page
     * 
     * @param htmlPage
     * @throws DotStateException
     * @throws DotDataException
     */
    public static void invalidateAll(IHTMLPage htmlPage) throws DotStateException, DotDataException, DotSecurityException {
        Identifier identifier = APILocator.getIdentifierAPI()
            .find(htmlPage);
        invalidate(htmlPage, identifier, false);
        invalidate(htmlPage, identifier, true);
    }

    @Override
    public void invalidate(Object obj) {
        IHTMLPage htmlPage = (IHTMLPage) obj;
        try {
            invalidateAll(htmlPage);
        } catch (DotStateException | DotDataException | DotSecurityException e) {
            throw new DotRuntimeException(e.getMessage());
        }
    }

    @Override
    public void invalidate(Object obj, boolean live) {
        IHTMLPage htmlPage = (IHTMLPage) obj;
        Identifier identifier;
        try {
            identifier = APILocator.getIdentifierAPI()
                .find(htmlPage);

            invalidate(htmlPage, identifier, live);
        } catch (DotDataException | DotSecurityException e) {
            throw new DotRuntimeException(e.getMessage());
        }
    }

    /**
     * Invalidates live html page
     * 
     * @param htmlPage
     * @throws DotStateException
     * @throws DotDataException
     */
    public static void invalidateLive(IHTMLPage htmlPage) throws DotStateException, DotDataException, DotSecurityException {
        Identifier identifier = APILocator.getIdentifierAPI()
            .find(htmlPage);
        invalidate(htmlPage, identifier, false);
    }

    /**
     * Invalidates working html page
     * 
     * @param htmlPage
     * @throws DotStateException
     * @throws DotDataException
     */
    public static void invalidateWorking(IHTMLPage htmlPage) throws DotStateException, DotDataException, DotSecurityException {
        Identifier identifier = APILocator.getIdentifierAPI()
            .find(htmlPage);
        invalidate(htmlPage, identifier, true);
    }

    private static void invalidate(IHTMLPage htmlPage, Identifier identifier, boolean EDIT_MODE)
            throws DotDataException, DotSecurityException {
        removePageFile(htmlPage, identifier, EDIT_MODE);

        if (htmlPage instanceof Contentlet) {
            if (EDIT_MODE) {
                ContentletServices.invalidateWorking((Contentlet) htmlPage, identifier);
            } else {
                ContentletServices.invalidateLive((Contentlet) htmlPage, identifier);
            }
        }
    }

    public static InputStream buildStream(IHTMLPage htmlPage, boolean EDIT_MODE, final String filePath) throws DotStateException, DotDataException {
        Identifier identifier = APILocator.getIdentifierAPI()
            .find(htmlPage);
        try {
            return buildStream(htmlPage, identifier, EDIT_MODE,   filePath);
        } catch (Exception e) {
            Logger.error(PageServices.class, e.getMessage(), e);
            throw new DotRuntimeException(e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public static InputStream buildStream(IHTMLPage htmlPage, Identifier identifier, boolean EDIT_MODE, final String filePath)
            throws DotDataException, DotSecurityException {
        String folderPath = (!EDIT_MODE) ? "live/" : "working/";
        InputStream result;
        StringBuilder sb = new StringBuilder();
        VersionableAPI vers = APILocator.getVersionableAPI();
        ContentletAPI conAPI = APILocator.getContentletAPI();
        Template cmsTemplate = APILocator.getHTMLPageAssetAPI()
            .getTemplate(htmlPage, EDIT_MODE);


        User sys = APILocator.systemUser();


        if (cmsTemplate == null || !InodeUtils.isSet(cmsTemplate.getInode())) {
            Logger.error(This.class, "PAGE DOES NOT HAVE A VALID TEMPLATE (template unpublished?) : page id "
                    + htmlPage.getIdentifier() + ":" + identifier.getURI());
        }



        // List of tags found in this page
        List<Tag> pageFoundTags = new ArrayList<>();

        // Check if we want to accrue the tags of this HTMLPage contentlet
        if (Config.getBooleanProperty("ACCRUE_TAGS_IN_PAGES", true)) {

            List<Tag> htmlPageFoundTags = APILocator.getTagAPI()
                .getTagsByInode(htmlPage.getInode());
            if (htmlPageFoundTags != null && !htmlPageFoundTags.isEmpty()) {
                pageFoundTags.addAll(htmlPageFoundTags);
            }
        }



        sb.append("#set($dotPageContent = $dotcontent.find(\"" + htmlPage.getInode() + "\" ))");


        // set the host variables

        Host host = APILocator.getHTMLPageAssetAPI()
            .getParentHost(htmlPage);
        sb.append("#if(!$doNotParseTemplate)")
            .append("#parse('")
            .append(folderPath)
            .append(host.getIdentifier())
            .append(".")
            .append(VelocityType.SITE.fileExtension)
            .append("')")
            .append(" #end ");



        Table<String, String, Set<String>> pageContents = new MultiTreeAPI().getPageMultiTrees(htmlPage, !EDIT_MODE);



        if (!pageContents.isEmpty()) {
            for (final String containerId : pageContents.rowKeySet()) {
                for (final String uniqueId : pageContents.row(containerId)
                    .keySet()) {
                    Set<String> cons = pageContents.get(containerId, uniqueId);

                    List<Contentlet> contentlets = conAPI.findContentletsByIdentifiers(cons.stream()
                        .toArray(String[]::new), !EDIT_MODE, -1, sys, false);


                    List<Contentlet> contentletsFull = (EDIT_MODE) ? contentlets
                            : conAPI.findContentletsByIdentifiers(cons.stream()
                                .toArray(String[]::new), false, -1, sys, false);



                    StringBuilder widgetpree = new StringBuilder();
                    StringBuilder widgetpreeFull = new StringBuilder();

                    StringBuilder contentletList = new StringBuilder();
                    int count = 0;
                    for (Contentlet contentlet : contentlets) {
                        contentletList.append(count++ == 0 ? "" : ",")
                            .append('"')
                            .append(contentlet.getIdentifier())
                            .append('"');
                        if (contentlet.getContentType()
                            .baseType() == BaseContentType.WIDGET) {
                            Field field = contentlet.getContentType()
                                .fieldMap()
                                .get("widgetPreexecute");
                            if (field != null && UtilMethods.isSet(field.values()))
                                widgetpree.append(field.values()
                                    .trim());
                        }
                    }

                    StringBuilder contentletListFull = new StringBuilder();
                    int countFull = 0;
                    for (Contentlet contentlet : contentletsFull) {
                        contentletListFull.append(countFull++ == 0 ? "" : ",")
                            .append('"')
                            .append(contentlet.getIdentifier())
                            .append('"');
                        if (contentlet.getContentType()
                            .baseType() == BaseContentType.WIDGET) {
                            Field field = contentlet.getContentType()
                                .fieldMap()
                                .get("widgetPreexecute");
                            if (field != null && UtilMethods.isSet(field.values()))
                                widgetpreeFull.append(field.values()
                                    .trim());
                        }
                    }

                    // Check if we want to accrue the tags associated to each contentlet on this
                    // page
                    if (Config.getBooleanProperty("ACCRUE_TAGS_IN_CONTENTS_ON_PAGE", false)) {
                        for (Contentlet contentlet : contentlets) {
                            // Search for the tags associated to this contentlet inode
                            List<Tag> contentletFoundTags = APILocator.getTagAPI()
                                .getTagsByInode(contentlet.getInode());
                            if (contentletFoundTags != null) {
                                pageFoundTags.addAll(contentletFoundTags);
                            }
                        }
                    }

                    sb.append("#if($UtilMethods.isSet($request.getSession(false)) && $request.session.getAttribute(\"tm_date\"))")
                        .append(widgetpreeFull)
                        .append("#set ($contentletList")
                        .append(containerId + uniqueId)
                        .append(" = [")
                        .append(contentletListFull.toString())
                        .append("] )")
                        .append("#set ($totalSize")
                        .append(containerId + uniqueId)
                        .append("=")
                        .append(contentletsFull.size())
                        .append(")")
                        .append("#else ")
                        .append(widgetpree)
                        .append("#set ($contentletList")
                        .append(containerId + uniqueId)
                        .append(" = [")
                        .append(contentletList.toString())
                        .append("] )")
                        .append("#set ($totalSize")
                        .append(containerId + uniqueId)
                        .append("=")
                        .append(contentlets.size())
                        .append(")")
                        .append("#end ");

                }
            }
        }

        // Now we need to use the found tags in order to accrue them each time this page is visited
        if (!pageFoundTags.isEmpty()) {
            // Velocity call to accrue tags on each request to this page
            sb.append("$tags.accrueTags(\"" + TagUtil.tagListToString(pageFoundTags) + "\" )");
        }

        if (htmlPage.isHttpsRequired()) {
            sb.append("#if(!$ADMIN_MODE  && !$request.isSecure())");
            sb.append("#if($request.getQueryString())");
            sb.append(
                    "#set ($REDIRECT_URL = \"https://${request.getServerName()}$request.getAttribute('javax.servlet.forward.request_uri')?$request.getQueryString()\")");
            sb.append("#else");
            sb.append(
                    "#set ($REDIRECT_URL = \"https://${request.getServerName()}$request.getAttribute('javax.servlet.forward.request_uri')\")");
            sb.append("#end");
            sb.append("$response.sendRedirect(\"$REDIRECT_URL\")");
            sb.append("#end");
        }

        sb.append("#if($HTMLPAGE_REDIRECT) && ${HTMLPAGE_REDIRECT}.length()>0");
        sb.append(" $response.setStatus(301)");
        sb.append(" $response.setHeader(\"Location\", \"$!HTMLPAGE_REDIRECT\")");
        sb.append("#end");

        Identifier iden = APILocator.getIdentifierAPI()
            .find(cmsTemplate);


        sb.append("#if(!$doNotParseTemplate)");
        if (cmsTemplate.isDrawed()) {// We have a designed template
            // Setting some theme variables
            sb.append("#set ($dotTheme = $templatetool.theme(\"")
                .append(cmsTemplate.getTheme())
                .append("\",\"")
                .append(host.getIdentifier())
                .append("\"))");
            sb.append("#set ($dotThemeLayout = $templatetool.themeLayout(\"")
                .append(cmsTemplate.getInode())
                .append("\" ))");
            // Merging our template
            sb.append("#parse(\"$dotTheme.templatePath\")");
        } else {
            sb.append("#parse('")
                .append(folderPath)
                .append(iden.getInode())
                .append(".")
                .append(VelocityType.TEMPLATE.fileExtension)
                .append("')");
        }
        sb.append("#end");


        try {

            if (Config.getBooleanProperty("SHOW_VELOCITYFILES", false)) {

                File f = new File(ConfigUtils.getDynamicVelocityPath() + java.io.File.separator + filePath);
                f.mkdirs();
                f.delete();
                java.io.BufferedOutputStream tmpOut = new java.io.BufferedOutputStream(Files
                    .newOutputStream(f.toPath()));
                // Specify a proper character encoding
                OutputStreamWriter out = new OutputStreamWriter(tmpOut, UtilMethods.getCharsetConfiguration());

                out.write(sb.toString());

                out.flush();
                out.close();
                tmpOut.close();
            }
        } catch (Exception e) {
            Logger.error(PageServices.class, e.toString(), e);
        }
        try {
            result = new ByteArrayInputStream(sb.toString()
                .getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e1) {
            result = new ByteArrayInputStream(sb.toString()
                .getBytes());
            Logger.error(ContainerServices.class, e1.getMessage(), e1);
        }
        return result;
    }

    public static void removePageFile(IHTMLPage htmlPage, Identifier identifier, boolean EDIT_MODE) {
        String folderPath = (!EDIT_MODE) ? "live" + java.io.File.separator : "working" + java.io.File.separator;
        String velocityRootPath = VelocityUtil.getVelocityRootPath();
        String languageStr = htmlPage.isContent() ? "_" + ((Contentlet) htmlPage).getLanguageId() : "";
        String filePath = folderPath + identifier.getId() + languageStr + "."
                + VelocityType.HTMLPAGE.fileExtension;
        velocityRootPath += java.io.File.separator;
        java.io.File f = new java.io.File(velocityRootPath + filePath);
        f.delete();
        DotResourceCache vc = CacheLocator.getVeloctyResourceCache2();
        vc.remove(ResourceManager.RESOURCE_TEMPLATE + filePath);
    }

    @Override
    public InputStream writeObject(String id1, String id2, boolean live, String language, final String filePath)
            throws DotDataException, DotSecurityException {

        HTMLPageAsset page = APILocator.getHTMLPageAssetAPI()
            .fromContentlet(APILocator.getContentletAPI()
                .findContentletByIdentifier(id1, live, Long.parseLong(language), sysUser(), true));


        return buildStream(page, !live, filePath);


    }


}
