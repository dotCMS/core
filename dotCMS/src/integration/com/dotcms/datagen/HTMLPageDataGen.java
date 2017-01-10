package com.dotcms.datagen;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.business.web.ContentletWebAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPIImpl;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import java.util.Map;

/**
 * Class used to create {@link Contentlet} objects of type HTMLPageAsset for test purposes
 *
 * @author Nollymar Longa
 */
public class HTMLPageDataGen extends ContentletDataGen {
    private long currentTime = System.currentTimeMillis();
    private long cacheTTL;
    private String seoKeywords;
    private String seoDescription;
    private boolean httpsRequired;
    private String metaData;
    private String pageURL = "test-page-url-" + currentTime;
    private String redirect;
    private String friendlyName = "test-page-friendly-name-" + currentTime;
    private String title = "test-page-title-" + currentTime;
    private boolean showOnMenu;
    private Template template;
    private long sortOrder = 1;
    private ContentletWebAPI contentletWebAPI;
    private HTMLPageAssetAPI pageAssetAPI;

    /**
     * Constructs a data-gen for building {@link HTMLPageAsset} objects.
     * <p>Created pages using the {@link #nextPersisted()} will live under the given host.
     *
     * @param host the host object
     */
    public HTMLPageDataGen(Host host, Template template) {
        this();
        this.host = host;
        this.template = template;
     }

    /**
     * Constructs a data-gen for building {@link HTMLPageAsset} objects.
     * <p>Created pages using the {@link #nextPersisted()} will live under the given folder.
     *
     * @param folder the folder object
     */
    public HTMLPageDataGen(Folder folder, Template template) {
        this();
        this.folder = folder;
        this.template = template;
    }

    private HTMLPageDataGen() {
        super(HTMLPageAssetAPIImpl.DEFAULT_HTMLPAGE_ASSET_STRUCTURE_INODE);
        contentletWebAPI = WebAPILocator.getContentletWebAPI();
        pageAssetAPI = APILocator.getHTMLPageAssetAPI();
    }

    @SuppressWarnings("unused")
    public HTMLPageDataGen cacheTTL(long cacheTTL) {
        this.cacheTTL = cacheTTL;
        return this;
    }

    @SuppressWarnings("unused")
    public HTMLPageDataGen seoKeywords(String seoKeywords) {
        this.seoKeywords = seoKeywords;
        return this;
    }

    @SuppressWarnings("unused")
    public HTMLPageDataGen seoDescription(String seoDescription) {
        this.seoDescription = seoDescription;
        return this;
    }

    @SuppressWarnings("unused")
    public HTMLPageDataGen httpsRequired(boolean httpsRequired) {
        this.httpsRequired = httpsRequired;
        return this;
    }

    @SuppressWarnings("unused")
    public HTMLPageDataGen metaData(String metaData) {
        this.metaData = metaData;
        return this;
    }

    @SuppressWarnings("unused")
    public HTMLPageDataGen pageURL(String pageURL) {
        this.pageURL = pageURL;
        return this;
    }

    @SuppressWarnings("unused")
    public HTMLPageDataGen redirect(String redirect) {
        this.redirect = redirect;
        return this;
    }

    public HTMLPageDataGen template(Template template) {
        this.template = template;
        return this;
    }

    @SuppressWarnings("unused")
    public HTMLPageDataGen friendlyName(String friendlyName) {
        this.friendlyName = friendlyName;
        return this;
    }

    public HTMLPageDataGen title(String title) {
        this.title = title;
        return this;
    }

    @SuppressWarnings("unused")
    public HTMLPageDataGen showOnMenu(boolean showOnMenu) {
        this.showOnMenu = showOnMenu;
        return this;
    }

    public HTMLPageDataGen languageId(long languageId){
        this.languageId = languageId;
        return this;
    }

    @SuppressWarnings("unused")
    public HTMLPageDataGen sortOrder(long sortOrder){
        this.sortOrder = sortOrder;
        return this;
    }

    public HTMLPageDataGen folder(Folder folder) {
        this.folder = folder;
        return this;
    }

    @Override
    public HTMLPageAsset next() {
        HTMLPageAsset htmlPageAsset = new HTMLPageAsset();
        htmlPageAsset.setFolder(folder.getInode());
        htmlPageAsset.setHost(host.getIdentifier());
        htmlPageAsset.setLanguageId(languageId);
        htmlPageAsset.setStructureInode(structureId);
        htmlPageAsset.setCacheTTL(cacheTTL);
        htmlPageAsset.setSeoKeywords(seoKeywords);
        htmlPageAsset.setSeoDescription(seoDescription);
        htmlPageAsset.setHttpsRequired(httpsRequired);
        htmlPageAsset.setMetadata(metaData);
        htmlPageAsset.setPageUrl(pageURL);
        htmlPageAsset.setRedirect(redirect);
        htmlPageAsset.setTemplateId(template.getIdentifier());
        htmlPageAsset.setFriendlyName(friendlyName);
        htmlPageAsset.setTitle(title);
        htmlPageAsset.setShowOnMenu(showOnMenu);
        htmlPageAsset.setSortOrder(sortOrder);

        for (Map.Entry<String, Object> element : properties.entrySet()) {
            htmlPageAsset.setProperty(element.getKey(), element.getValue());
        }

        return htmlPageAsset;
    }

    @Override
    public HTMLPageAsset nextPersisted() {
        HTMLPageAsset pageAsset = next();

        String status = contentletWebAPI.validateNewContentPage(pageAsset);

        if (UtilMethods.isSet(status)) {
            String msg = "Error validating htmlpage";
            try {
                msg = LanguageUtil.get(user, status);
            } catch (LanguageException e) {
                Logger.debug(this, "Error getting property from LanguageUtil.get", e);
            }
            throw new DotRuntimeException(msg);
        }

        Contentlet contentlet = persist(pageAsset);
        return pageAssetAPI.fromContentlet(contentlet);
    }

}
