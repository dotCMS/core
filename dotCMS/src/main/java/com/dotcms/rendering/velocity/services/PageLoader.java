package com.dotcms.rendering.velocity.services;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.TagUtil;
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

        
        
        for (PageMode mode : PageMode.values()) {
            invalidate(obj, mode);
        }


    }

    @Override
    public void invalidate(final Object obj, final PageMode mode) {
        HTMLPageAsset htmlPage =null;
        if(obj instanceof IHTMLPage) {
            htmlPage = (HTMLPageAsset) obj;
        }else if(obj instanceof Contentlet) {
             htmlPage = APILocator.getHTMLPageAssetAPI().fromContentlet((Contentlet) obj);
        }
       if(htmlPage==null) {
           return;
       }
        


        for(Language lang : APILocator.getLanguageAPI().getLanguages()) {
            VelocityResourceKey key = new VelocityResourceKey(htmlPage, mode, lang.getId());
            DotResourceCache vc = CacheLocator.getVeloctyResourceCache();
            vc.remove(key);
        }

    }





    public InputStream buildStream(IHTMLPage htmlPage, PageMode mode, final String filePath)
            throws DotDataException, DotSecurityException {
        String folderPath = mode.name() + File.separator;

        StringBuilder sb = new StringBuilder();


        Template cmsTemplate = APILocator.getHTMLPageAssetAPI()
            .getTemplate(htmlPage, !mode.showLive);


        User sys = APILocator.systemUser();


        if (cmsTemplate == null || !InodeUtils.isSet(cmsTemplate.getInode())) {
            throw new DotStateException("PAGE DOES NOT HAVE A VALID TEMPLATE (template unpublished?) : page id "
                    + htmlPage.getIdentifier() + " template id" + htmlPage.getTemplateId());
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

        // Add the pre-execute code of a widget to the page, regardless the mode
        final PageRenderUtil pce = new PageRenderUtil((HTMLPageAsset) htmlPage, sys, mode);
        sb.append(pce.getWidgetPreExecute());

        /**
         * Serializes the page variables and pointers to 
         * the content (multitree entries)
         * in the page velocity template
         */
        if (mode == PageMode.LIVE) {
          sb.append(pce.asString());
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

        sb.append("#if($HTMLPAGE_REDIRECT != \"\")")
            .append("$response.setStatus(301)")
            .append("$response.setHeader(\"Location\", \"$HTMLPAGE_REDIRECT\")")
            .append("#end");


        sb.append("#if(!$doNotParseTemplate)");
        if (cmsTemplate.isDrawed()) {
            
            if(null == cmsTemplate.getTheme()) {
                throw new DotStateException("Drawed template has no theme.  Template id: " + cmsTemplate.getIdentifier() + " template name:" + cmsTemplate.getName());
            }
            
            
            // We have a designed template
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

    public InputStream writeObject(final VelocityResourceKey key) throws DotDataException, DotSecurityException {

        HTMLPageAsset page = APILocator.getHTMLPageAssetAPI()
            .fromContentlet(APILocator.getContentletAPI()
                .findContentletByIdentifier(key.id1, key.mode.showLive, Long.parseLong(key.language), sysUser(), true));


        return buildStream(page, key.mode, key.path);


    }


}
