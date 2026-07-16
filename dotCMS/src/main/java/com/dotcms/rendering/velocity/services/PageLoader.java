package com.dotcms.rendering.velocity.services;

import com.dotcms.util.ConversionUtils;
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
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

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
        invalidate(obj, null, mode);
    }

    public void invalidate(final Object obj, final String variantName, final PageMode mode) {
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
            VelocityResourceKey key = UtilMethods.isSet(variantName) ?
                    new VelocityResourceKey(htmlPage, mode, lang.getId(), variantName) :
                    new VelocityResourceKey(htmlPage, mode, lang.getId());
            DotResourceCache vc = CacheLocator.getVeloctyResourceCache();
            vc.remove(key);
        }

    }

    /**
     * Builds the Velocity stream for the given HTML page using the page's own language as the
     * viewing language. Preserved for OSGi backward compatibility — new code should prefer
     * {@link #buildStream(IHTMLPage, PageMode, String, long)}.
     *
     * @param htmlPage The HTML page to build the stream for.
     * @param mode     The page mode (LIVE, PREVIEW, EDIT, etc.).
     * @param filePath The file path used as the Velocity resource key.
     * @return An {@link InputStream} with the compiled Velocity content.
     * @throws DotDataException     If there is an issue retrieving data from the DB.
     * @throws DotSecurityException If the current user doesn't have the required permissions.
     */
    public InputStream buildStream(final IHTMLPage htmlPage, final PageMode mode,
            final String filePath) throws DotDataException, DotSecurityException {
        return buildStream(htmlPage, mode, filePath, htmlPage.getLanguageId());
    }

    public InputStream buildStream(IHTMLPage htmlPage, PageMode mode, final String filePath,
            final long viewingLanguageId) throws DotDataException, DotSecurityException {
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

        addWidgetPreExecuteCodeAndPageInfo((HTMLPageAsset) htmlPage, mode, sb, sys, viewingLanguageId);

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
            // Merging our template. File-asset themes are included with #dotParse so the theme
            // template.vtl is resolved version-aware (live vs working) from the page's PageMode;
            // the bundled static fallback templates are plain disk files and stay on #parse.
            // dontShowThemeTemplateIcon is a one-shot flag consumed by #dotParse so the theme shell
            // itself is not wrapped in an EDIT_MODE edit-control icon; nested #dotParse includes
            // (html_head/header/footer) inside the theme still emit their icons.
            sb.append("#if($dotTheme.templatePathIsFileAsset)")
                .append("#set($dontShowThemeTemplateIcon = true)")
                .append("#dotParse(\"$dotTheme.templatePath\")")
                .append("#else")
                .append("#parse(\"$dotTheme.templatePath\")")
                .append("#end");
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

    private void addWidgetPreExecuteCodeAndPageInfo(final HTMLPageAsset htmlPage, final PageMode mode,
            final StringBuilder stringBuilder, final User user, final long viewingLanguageId) throws DotSecurityException, DotDataException {
        final Host host = APILocator.getHostAPI().find(htmlPage.getHost(), user, false);
        final PageRenderUtil pce = new PageRenderUtil(htmlPage, user, mode, viewingLanguageId, host);
        // Add the pre-execute code of a widget to the page
        stringBuilder.append(pce.getWidgetPreExecute());
        // Adds the page info
        stringBuilder.append(pce.asString());
    }


    @Override

    public InputStream writeObject(final VelocityResourceKey key) throws DotDataException, DotSecurityException {
        // Use the language from the velocity key (the requested viewing language) instead of the
        // page's own language. When a page falls back to the default language due to
        // DEFAULT_PAGE_TO_DEFAULT_LANGUAGE, the page's languageId becomes the default lang, but
        // content lookup should still use the originally-requested language so that contentlets
        // existing only in the requested language (e.g. for site search indexing) are rendered.
        return buildStream(getPage(key), key.mode, key.path, ConversionUtils.toLong(key.language));
    }

    private HTMLPageAsset getPage(VelocityResourceKey key)
            throws DotDataException, DotSecurityException {

        return (HTMLPageAsset) APILocator.getHTMLPageAssetAPI().findByIdLanguageVariantFallback(key.id1, ConversionUtils.toLong(key.language), key.variant, key.mode.showLive, sysUser(), true);


    }


}
