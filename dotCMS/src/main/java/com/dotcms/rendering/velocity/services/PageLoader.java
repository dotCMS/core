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
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
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
public class PageLoader implements VelocityCMSObject {

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
    public void invalidateLive(IHTMLPage htmlPage) throws DotStateException, DotDataException, DotSecurityException {
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
    public void invalidateWorking(IHTMLPage htmlPage) throws DotStateException, DotDataException, DotSecurityException {
        Identifier identifier = APILocator.getIdentifierAPI()
            .find(htmlPage);
        invalidate(htmlPage, identifier, true);
    }

    private static void invalidate(IHTMLPage htmlPage, Identifier identifier, boolean EDIT_MODE)
            throws DotDataException, DotSecurityException {
        removePageFile(htmlPage, identifier, EDIT_MODE);

        if (htmlPage instanceof Contentlet) {
            if (EDIT_MODE) {
                new ContentletLoader().invalidateWorking((Contentlet) htmlPage, identifier);
            } else {
                new ContentletLoader().invalidateLive((Contentlet) htmlPage, identifier);
            }
        }
    }

    public InputStream buildStream(IHTMLPage htmlPage, boolean EDIT_MODE, final String filePath)
            throws DotStateException, DotDataException {
        Identifier identifier = APILocator.getIdentifierAPI()
            .find(htmlPage);
        try {
            return buildStream(htmlPage, identifier, EDIT_MODE, filePath);
        } catch (Exception e) {
            Logger.error(this.getClass(), e.getMessage(), e);
            throw new DotRuntimeException(e.getMessage());
        }
    }


    public InputStream buildStream(IHTMLPage htmlPage, Identifier identifier, boolean EDIT_MODE, final String filePath)
            throws DotDataException, DotSecurityException {
        String folderPath = (!EDIT_MODE) ? "live/" : "working/";
        InputStream result;
        StringBuilder sb = new StringBuilder();

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

        PageMode mode = (EDIT_MODE) ? PageMode.PREVIEW :PageMode.LIVE;
        
        sb.append( new PageVelocityContext(htmlPage, sys, mode).printForVelocity());
        
        
        



        try {
            result = new ByteArrayInputStream(sb.toString()
                .getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e1) {
            result = new ByteArrayInputStream(sb.toString()
                .getBytes());
            Logger.error(this.getClass(), e1.getMessage(), e1);
        }
        return result;
    }

    public static void removePageFile(IHTMLPage htmlPage, Identifier identifier, boolean EDIT_MODE) {
        String folderPath = (!EDIT_MODE) ? "live" + java.io.File.separator : "working" + java.io.File.separator;
        String velocityRootPath = VelocityUtil.getVelocityRootPath();
        String languageStr = htmlPage.isContent() ? "_" + ((Contentlet) htmlPage).getLanguageId() : "";
        String filePath = folderPath + identifier.getId() + languageStr + "." + VelocityType.HTMLPAGE.fileExtension;
        velocityRootPath += java.io.File.separator;
        java.io.File f = new java.io.File(velocityRootPath + filePath);
        f.delete();
        DotResourceCache vc = CacheLocator.getVeloctyResourceCache();
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
