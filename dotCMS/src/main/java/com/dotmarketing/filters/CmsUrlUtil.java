package com.dotmarketing.filters;

import com.dotcms.services.VanityUrlServices;
import com.dotcms.vanityurl.business.VanityUrlAPI;
import com.dotcms.vanityurl.model.CachedVanityUrl;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Versionable;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;

public class CmsUrlUtil {

    private static CmsUrlUtil urlUtil;
    private static final String CONTENTLET = "contentlet";
    private static final String HTMLPAGE = "htmlpage";
    private static final String FILE_ASSET = "file_asset";
    private static final String FOLDER = "folder";
    private static final String NOT_FOUND = "NOTFOUND";
    private static final String UNABLE_TO_FIND = "Unable to find ";

    public static CmsUrlUtil getInstance() {
        if (urlUtil == null) {

            synchronized (CmsUrlUtil.class) {
                urlUtil = new CmsUrlUtil();
            }

        }
        return urlUtil;
    }


    public boolean isPageAsset(Versionable asset) {
        try {
            Identifier id = APILocator.getIdentifierAPI().find(asset);
            if (CONTENTLET.equals(id.getAssetType()) && asset instanceof Contentlet) {
                Contentlet c = (Contentlet) asset;
                return (c.getStructure().getStructureType() == Structure.STRUCTURE_TYPE_HTMLPAGE);
            } else if (HTMLPAGE.equals(id.getAssetType())) {
                return true;
            }

            return false;
        } catch (Exception e) {
            throw new DotStateException("Getting id failed" + e.getMessage(), e);
        }

    }

    public boolean isPageAsset(String uri, Host host, Long languageId) {
        Identifier id;
        if (!UtilMethods.isSet(uri)) {
            return false;
        }
        try {
            id = APILocator.getIdentifierAPI().find(host, uri);
        } catch (Exception e) {
            Logger.error(this.getClass(), UNABLE_TO_FIND + uri);
            return false;
        }
        if (id == null || id.getId() == null) {
            return false;
        }
        if (HTMLPAGE.equals(id.getAssetType())) {
            return true;
        }
        if (CONTENTLET.equals(id.getAssetType())) {
            try {

                //Get the list of languages use by the application
                List<Language> languages = APILocator.getLanguageAPI().getLanguages();

                //First try with the given language
                ContentletVersionInfo cinfo = APILocator.getVersionableAPI()
                        .getContentletVersionInfo(id.getId(), languageId);
                if (cinfo == null || cinfo.getWorkingInode().equals(NOT_FOUND)) {

                    for (Language language : languages) {
                        /*
						If we found nothing with the given language it does not mean is not a page,
						could be just a page but it does not exist for the given language.
						Trying with the other languages use in the app.
						 */
                        if (languageId != language.getId()) {
                            cinfo = APILocator.getVersionableAPI()
                                    .getContentletVersionInfo(id.getId(), language.getId());
                            if (cinfo != null && !cinfo.getWorkingInode().equals(NOT_FOUND)) {
                                //Found it
                                break;
                            }
                        }
                    }

                }
                if (cinfo == null || cinfo.getWorkingInode().equals(NOT_FOUND)) {
                    return false;//At this point we know is not a page
                } else {
                    Contentlet c = APILocator.getContentletAPI()
                            .find(cinfo.getWorkingInode(), APILocator.getUserAPI().getSystemUser(),
                                    false);
                    return (c.getStructure().getStructureType()
                            == Structure.STRUCTURE_TYPE_HTMLPAGE);
                }
            } catch (Exception e) {
                Logger.error(this.getClass(), UNABLE_TO_FIND + uri);
                return false;
            }
        }
        return false;
    }

    public boolean isFileAsset(String uri, Host host, Long languageId) {

        // languageId is not used now, but will be used in future functionality. Issue #7141

        Identifier id;
        try {
            id = APILocator.getIdentifierAPI().find(host, uri);
        } catch (Exception e) {
            Logger.error(this.getClass(), UNABLE_TO_FIND + uri);
            return false;
        }
        if (id == null || id.getId() == null) {
            return false;
        }
        if (FILE_ASSET.equals(id.getAssetType())) {
            return true;
        }

        if (CONTENTLET.equals(id.getAssetType())) {
            try {
                ContentletVersionInfo cinfo = APILocator.getVersionableAPI()
                        .getContentletVersionInfo(id.getId(), languageId);

                if ((cinfo == null || cinfo.getWorkingInode().equals(NOT_FOUND)) && Config
                        .getBooleanProperty("DEFAULT_FILE_TO_DEFAULT_LANGUAGE", false)) {
                    //Get the Default Language
                    Language defaultLang = APILocator.getLanguageAPI().getDefaultLanguage();
                    //If the fallback to Default Language is set to true, let's see if the requested file is stored with Default Language
                    cinfo = APILocator.getVersionableAPI()
                            .getContentletVersionInfo(id.getId(), defaultLang.getId());
                }

                if (cinfo == null || cinfo.getWorkingInode().equals(NOT_FOUND)) {
                    return false;//At this point we know is not a File Asset
                } else {
                    Contentlet c = APILocator.getContentletAPI()
                            .find(cinfo.getWorkingInode(), APILocator.getUserAPI().getSystemUser(),
                                    false);
                    return (c.getStructure().getStructureType()
                            == Structure.STRUCTURE_TYPE_FILEASSET);
                }
            } catch (Exception e) {
                Logger.error(this.getClass(), UNABLE_TO_FIND + uri);
                return false;
            }
        }
        return false;
    }

    public boolean isFolder(String uri, Host host) {
        Identifier id;
        if ("/".equals(uri)) {
            return true;
        }
        while (uri.endsWith("/") && uri.length() > 1) {
            uri = uri.substring(0, uri.length() - 1);
        }

        if (!uri.startsWith("/")) {
            uri = "/" + uri;
        }

        try {
            id = APILocator.getIdentifierAPI().find(host, uri);
            if (id == null || id.getId() == null) {
                return false;
            }
            if (FOLDER.equals(id.getAssetType())) {
                return true;
            }
        } catch (Exception e) {
            Logger.error(this.getClass(), UNABLE_TO_FIND + uri);
        }

        return false;
    }

    public boolean isVanityUrl(String uri, Host host, long languageId) {

        if (uri.length() > 1 && uri.endsWith("/")) {
            uri = uri.substring(0, uri.length() - 1);
        }

        CachedVanityUrl cachedVanityUrl = getCachedVanityUrl(uri, host, languageId);
        boolean isVanityURL =
                UtilMethods.isSet(cachedVanityUrl) && !cachedVanityUrl.getVanityUrlId()
                        .equals(VanityUrlAPI.CACHE_404_VANITY_URL);

        if (!isVanityURL) {
            cachedVanityUrl = getCachedVanityUrl(uri, null, languageId);
            isVanityURL = UtilMethods.isSet(cachedVanityUrl) && !cachedVanityUrl.getVanityUrlId()
                    .equals(VanityUrlAPI.CACHE_404_VANITY_URL);
        }
        // Still support legacy cmsHomePage
        if ("/".equals(uri) && !isVanityURL) {
            uri = "/cmsHomePage";
            cachedVanityUrl = getCachedVanityUrl(uri, host, languageId);
            isVanityURL = UtilMethods.isSet(cachedVanityUrl) && !cachedVanityUrl.getVanityUrlId()
                    .equals(VanityUrlAPI.CACHE_404_VANITY_URL);

            if (!isVanityURL) {
                cachedVanityUrl = getCachedVanityUrl(uri, null, languageId);
                isVanityURL =
                        UtilMethods.isSet(cachedVanityUrl) && !cachedVanityUrl.getVanityUrlId()
                                .equals(VanityUrlAPI.CACHE_404_VANITY_URL);
            }
        }

        return isVanityURL;

    }

    public boolean canRead(Identifier ident, long languageId, User user) {
        if (ident == null || ident.getId() == null) {
            throw new DotStateException("Identifier cannot be null");
        }
        if (ident.getAssetType().equals(CONTENTLET)) {
            try {
                ContentletVersionInfo cinfo = APILocator.getVersionableAPI()
                        .getContentletVersionInfo(ident.getId(), languageId);

                // If we did not find a version with for given
                // language lets try with the default language
                if (cinfo == null && languageId != APILocator.getLanguageAPI().getDefaultLanguage()
                        .getId()) {
                    languageId = APILocator.getLanguageAPI().getDefaultLanguage().getId();
                    cinfo = APILocator.getVersionableAPI()
                            .getContentletVersionInfo(ident.getId(), languageId);
                }

                Contentlet proxy = new Contentlet();

                proxy.setInode(cinfo.getWorkingInode());
                proxy.setIdentifier(cinfo.getIdentifier());
                proxy.setLanguageId(cinfo.getLang());
                return APILocator.getPermissionAPI()
                        .doesUserHavePermission(proxy, PermissionAPI.PERMISSION_READ, user, true);
            } catch (Exception e) {
                Logger.warn(this,
                        "Unable to find file asset contentlet with identifier " + ident.getId(), e);
            }
        }

        return false;
    }

    public boolean amISomething(String uri, Host host, Long languageId) {
        return (urlUtil.isFileAsset(uri, host, languageId) || urlUtil
                .isVanityUrl(uri, host, languageId)
                || urlUtil.isPageAsset(uri, host, languageId) || urlUtil.isFolder(uri, host));
    }

    /**
     * Search for the Cached Vanity Url
     *
     * @param uri The URI to match
     * @param host The current host
     * @param languageId The current languageId
     * @return The cached vanity Url
     */
    public CachedVanityUrl getCachedVanityUrl(String uri, Host host, long languageId) {
        CachedVanityUrl result = null;

        if (host == null) {
            try {
                host = APILocator.getHostAPI().findSystemHost();
            } catch (DotDataException e) {
                Logger.error(this, "Error searching for System host", e);
            }
        }
        //Search fot the URI in the vanityURL cached cache
        Set<CachedVanityUrl> cachedVanityUrls = CacheLocator.getVanityURLCache()
                .getCachedVanityUrls(host);
        if (cachedVanityUrls.isEmpty()) {
            //Initialize the Cached Vanity URL cache
            VanityUrlServices.initializeVanityUrlCache();
            cachedVanityUrls = CacheLocator.getVanityURLCache().getCachedVanityUrls(host);
        }
        for (CachedVanityUrl vanity : cachedVanityUrls) {
            if (vanity != null) {
                Matcher matcher = vanity.getPattern().matcher(uri);
                if (matcher.matches()) {
                    result = vanity;
                    break;
                }
            } else {
                Logger.debug(this,
                        "Error this URI:" + uri + " has an invalid Vanity URL value:" + vanity);
            }
        }
        if (result == null) {
            result = APILocator.getVanityUrlAPI()
                    .getLiveCachedVanityUrl(uri, host, languageId, APILocator.systemUser());
        }

        return result;
    }
}
