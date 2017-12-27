package com.dotcms.rendering.velocity.services;

import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.dotcms.repackage.bsh.This;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.TagUtil;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.velocity.runtime.resource.ResourceManager;

import com.liferay.portal.model.User;

/**
 * @author will
 *
 *         To change this generated comment edit the template variable "typecomment":
 *         Window>Preferences>Java>Templates. To enable and disable the creation of type comments go
 *         to Window>Preferences>Java>Code Generation.
 */
public class PageLoader implements DotLoader {



    @Override
    public void invalidate(Object obj) {
        IHTMLPage htmlPage = (IHTMLPage) obj;
        for (PageMode mode : PageMode.values()) {
            invalidate(htmlPage, mode);
        }


    }

    @Override
    public void invalidate(Object obj, PageMode mode) {
        HTMLPageAsset htmlPage =null;
        if(obj instanceof IHTMLPage) {
            htmlPage = (HTMLPageAsset) obj;
        }else if(obj instanceof Contentlet) {
             htmlPage = APILocator.getHTMLPageAssetAPI().fromContentlet((Contentlet) obj);
        }
       if(htmlPage==null) {
           return;
       }
        


        String folderPath = mode.name() + File.separator;
        String velocityRootPath = VelocityUtil.getVelocityRootPath();
        String languageStr = htmlPage.getLanguageId() +"";
        String filePath = folderPath + htmlPage.getIdentifier() + languageStr + "." + VelocityType.HTMLPAGE.fileExtension;
        velocityRootPath += java.io.File.separator;
        java.io.File f = new java.io.File(velocityRootPath + filePath);
        f.delete();
        DotResourceCache vc = CacheLocator.getVeloctyResourceCache();
        vc.remove(ResourceManager.RESOURCE_TEMPLATE + filePath);

    }



    public InputStream buildStream(IHTMLPage htmlPage, PageMode mode, final String filePath)
            throws DotStateException, DotDataException {
        Identifier identifier = APILocator.getIdentifierAPI()
            .find(htmlPage);
        try {
            return buildStream(htmlPage, identifier, mode, filePath);
        } catch (Exception e) {
            Logger.error(this.getClass(), e.getMessage(), e);
            throw new DotRuntimeException(e.getMessage());
        }
    }


    public InputStream buildStream(IHTMLPage htmlPage, Identifier identifier, PageMode mode, final String filePath)
            throws DotDataException, DotSecurityException {
        String folderPath = mode.name() + File.separator;
        InputStream result;
        StringBuilder sb = new StringBuilder();

        ContentletAPI conAPI = APILocator.getContentletAPI();
        Template cmsTemplate = APILocator.getHTMLPageAssetAPI()
            .getTemplate(htmlPage, mode.showLive);


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
            .append("#end");


        sb.append(new PageContextBuilder(htmlPage, sys, mode).asString());


        // Now we need to use the found tags in order to accrue them each time this page is visited
        if (!pageFoundTags.isEmpty()) {
            // Velocity call to accrue tags on each request to this page
            sb.append("$tags.accrueTags(\"" + TagUtil.tagListToString(pageFoundTags) + "\" )");
        }
        if (htmlPage.isHttpsRequired()) {
            sb.append(" #if(!$ADMIN_MODE  && !$request.isSecure())");
            sb.append("#if($request.getQueryString())");
            sb.append("#set ($REDIRECT_URL = \"https://${request.getServerName()}$request.getAttribute('javax.servlet.forward.request_uri')?$request.getQueryString()\")");
            sb.append("#else");
            sb.append("#set ($REDIRECT_URL = \"https://${request.getServerName()}$request.getAttribute('javax.servlet.forward.request_uri')\")");
            sb.append("#end");
            sb.append("$response.sendRedirect(\"$REDIRECT_URL\")");
            sb.append("#end");
        }

        sb.append("#if($HTMLPAGE_REDIRECT != \"\")");
        sb.append("$response.setStatus(301)");
        sb.append("$response.setHeader(\"Location\", \"$HTMLPAGE_REDIRECT\")");
        sb.append("#end");


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
                .append("\"))");
            // Merging our template
            sb.append("#parse(\"$dotTheme.templatePath\")");
        } else {
            sb.append("#parse('")
                .append(folderPath)
                .append(cmsTemplate.getIdentifier())
                .append(".")
                .append(VelocityType.TEMPLATE.fileExtension)
                .append("')");
        }
        sb.append("#end");

        return writeOutVelocity(filePath, sb.toString());


    }


    @Override
    public InputStream writeObject(String id1, String id2, PageMode mode, String language, final String filePath)
            throws DotDataException, DotSecurityException {

        HTMLPageAsset page = APILocator.getHTMLPageAssetAPI()
            .fromContentlet(APILocator.getContentletAPI()
                .findContentletByIdentifier(id1, mode.showLive, Long.parseLong(language), sysUser(), true));


        return buildStream(page, mode, filePath);


    }


}
