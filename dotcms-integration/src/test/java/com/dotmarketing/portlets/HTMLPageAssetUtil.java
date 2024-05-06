package com.dotmarketing.portlets;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPIImpl;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.templates.model.Template;
import com.liferay.portal.model.User;

/**
 * Created by Oscar Arrieta on 2/24/16.
 */
public class HTMLPageAssetUtil {

    /**
     * Creates a dummy page as Content Pages.
     *
     * @param friendlyName
     *            - The human-readable page name.
     * @param URL
     *            - The name of the page in the URL.
     * @param title
     *            - The title of the HTML page.
     * @param template
     *            - The template that provides the layout of the page.
     * @param folder
     *            - The folder where the page will be placed.
     * @param host
     *            - The site where the page will be created.
     *
     * @return The new {@link IHTMLPage} page.
     *
     * @throws DotDataException
     *             An error occurred when inserting information in the database.
     * @throws DotSecurityException
     *             The specified user does not have permission to perform the
     *             page creation action.
     */
    public static HTMLPageAsset createDummyPage(String friendlyName, String URL, String title, Template template, Folder folder, Host host) throws DotDataException, DotSecurityException{
        User sysuser = APILocator.getUserAPI().getSystemUser();

        Contentlet contentAsset=new Contentlet();
        contentAsset.setStructureInode(HTMLPageAssetAPIImpl.DEFAULT_HTMLPAGE_ASSET_STRUCTURE_INODE);
        contentAsset.setHost(host.getIdentifier());
        contentAsset.setProperty(HTMLPageAssetAPIImpl.FRIENDLY_NAME_FIELD, friendlyName);
        contentAsset.setProperty(HTMLPageAssetAPIImpl.URL_FIELD, URL);
        contentAsset.setProperty(HTMLPageAssetAPIImpl.TITLE_FIELD, title);
        contentAsset.setProperty(HTMLPageAssetAPIImpl.CACHE_TTL_FIELD, "0");
        contentAsset.setProperty(HTMLPageAssetAPIImpl.TEMPLATE_FIELD, template.getIdentifier());
        contentAsset.setFolder(folder.getInode());
        contentAsset = APILocator.getContentletAPI().checkin(contentAsset, sysuser, false);

        return APILocator.getHTMLPageAssetAPI().fromContentlet(contentAsset);
    }
}
