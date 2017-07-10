package com.dotcms.vanityurl.business;

import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.VanityUrlContentType;
import com.dotcms.services.VanityUrlServices;
import com.dotcms.util.VanityUrlUtil;
import com.dotcms.vanityurl.model.CachedVanityUrl;
import com.dotcms.vanityurl.model.DefaultVanityUrl;
import com.dotcms.vanityurl.model.VanityUrl;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletValidationException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;

/**
 * Implementation class for the {@link VanityUrlAPI}.
 *
 * @author oswaldogallango
 * @version 4.2.0
 * @since June 12, 2017
 */
public class VanityUrlAPIImpl implements VanityUrlAPI {

    private final ContentletAPI contentletAPI = APILocator.getContentletAPI();
    private final VanityUrl cache404VanityUrl = new DefaultVanityUrl();

    private static final String GET_VANITY_URL_BASE_TYPE =
            "+baseType:" + BaseContentType.VANITY_URL.getType();
    private static final String GET_ALL_VANITY_URL = GET_VANITY_URL_BASE_TYPE + " +working:true";
    private static final String GET_ACTIVE_VANITY_URL =
            GET_VANITY_URL_BASE_TYPE + " +live:true +deleted:false";
    private static final String GET_VANITY_URL_LANGUAGE_ID = " +languageId:";
    private static final int CODE_404_VALUE = 404;

    public VanityUrlAPIImpl() {
        cache404VanityUrl.setInode(VanityUrlAPI.CACHE_404_VANITY_URL);
        cache404VanityUrl.setIdentifier(VanityUrlAPI.CACHE_404_VANITY_URL);
        cache404VanityUrl.setAction(CODE_404_VALUE);
    }

    @Override
    public List<VanityUrl> getAllVanityUrls(final User user) {
        ImmutableList.Builder<VanityUrl> results = ImmutableList.builder();
        TreeSet vanityTreeSet = new TreeSet<VanityUrl>();
        try {
            List<Contentlet> contentResults = contentletAPI
                    .search(GET_ALL_VANITY_URL,
                            0, 0, StringPool.BLANK, user, false);
            contentResults.stream()
                    .forEach((Contentlet con) -> vanityTreeSet.add(getVanityUrlFromContentlet(con)));
        } catch (DotDataException | DotSecurityException e) {
            Logger.error(this, "Error searching for all Vanity URLs", e);
        }

        return results.addAll(vanityTreeSet).build();
    }

    @Override
    public List<VanityUrl> getActiveVanityUrls(final User user) {
        ImmutableList.Builder<VanityUrl> results = new ImmutableList.Builder();
        TreeSet vanityTreeSet = new TreeSet<VanityUrl>();
        try {
            List<Contentlet> contentResults = contentletAPI
                    .search(GET_ACTIVE_VANITY_URL, 0, 0, StringPool.BLANK, user, false);
            contentResults.stream().forEach((Contentlet con) -> {
                VanityUrl vanityUrl = getVanityUrlFromContentlet(con);
                addToVanityURLCache(vanityUrl);
                vanityTreeSet.add(vanityUrl);
            });
        } catch (DotDataException | DotSecurityException e) {
            Logger.error(this, "Error searching for active Vanity URLs", e);
        }
        return results.addAll(vanityTreeSet).build();
    }

    @Override
    public List<VanityUrl> getActiveVanityUrlsByHostAndLanguage(final String hostId,
            final long languageId, final User user) {
        ImmutableList.Builder<VanityUrl> results = new ImmutableList.Builder();
        TreeSet vanityTreeSet = new TreeSet<VanityUrl>();
        try {
            List<Contentlet> contentResults = contentletAPI
                    .search(GET_ACTIVE_VANITY_URL + " +conHost:" + hostId
                            + GET_VANITY_URL_LANGUAGE_ID
                            + languageId, 0, 0, StringPool.BLANK, user, false);
            contentResults.stream().forEach((Contentlet con) -> {
                VanityUrl vanityUrl = getVanityUrlFromContentlet(con);
                addToVanityURLCache(vanityUrl);
                vanityTreeSet.add(vanityUrl);
            });
        } catch (DotDataException | DotSecurityException e) {
            Logger.error(this, "Error searching for active Vanity URLs", e);
        }
        return results.addAll(vanityTreeSet).build();
    }

    @Override
    public List<CachedVanityUrl> getActiveCachedVanityUrls(final User user) {
        ImmutableList.Builder<CachedVanityUrl> results = new ImmutableList.Builder();
        List<VanityUrl> vanityUrls = getActiveVanityUrls(user);
        vanityUrls.stream().forEach((VanityUrl vanityUrl) -> {
            addToVanityURLCache(vanityUrl);
            results.add(new CachedVanityUrl(vanityUrl));
        });
        return results.build();
    }

    @Override
    public VanityUrl getVanityUrlFromContentlet(final Contentlet con) {
        if (con != null) {
            try {
                if (!con.isVanityUrl()) {
                    throw new DotStateException(
                            "Contentlet : " + con.getInode() + " is not a Vanity Url");
                }
            } catch (DotDataException | DotSecurityException e) {
                throw new DotStateException(
                        "Contentlet : " + con.getInode() + " is not a Vanity Url", e);
            }
        } else {
            throw new DotStateException("Contentlet is null");
        }

        DefaultVanityUrl vanityUrl = new DefaultVanityUrl();
        vanityUrl.setStructureInode(con.getContentTypeId());
        try {
            contentletAPI.copyProperties((Contentlet) vanityUrl, con.getMap());
        } catch (Exception e) {
            throw new DotStateException("Vanity Url Copy Failed", e);
        }
        vanityUrl.setHost(con.getHost());
        if (UtilMethods.isSet(con.getFolder())) {
            try {
                Folder folder = APILocator.getFolderAPI()
                        .find(con.getFolder(), APILocator.systemUser(), false);
                vanityUrl.setFolder(folder.getInode());
            } catch (Exception e) {
                vanityUrl = new DefaultVanityUrl();
                Logger.warn(this, "Unable to convert contentlet to Vanity Url " + con, e);
            }
        }

        return vanityUrl;
    }

    /**
     * Add the Vanity URL to the vanityURLCache
     *
     * @param vanityUrl The vanityurl URL object
     */
    private void addToVanityURLCache(VanityUrl vanityUrl) {
        try {
            if (vanityUrl.isLive()) {
                VanityUrlServices.getInstance().updateCache(vanityUrl);
            } else {
                VanityUrlServices.getInstance().invalidateVanityUrl(vanityUrl);
            }
        } catch (DotDataException | DotRuntimeException | DotSecurityException e) {
            Logger.error(this,
                    "Error trying to add Vanity URL identifier:" + vanityUrl.getIdentifier()
                            + " to VanityURLCache", e);
        }
    }

    /**
     * Add a 404 Vanity URL to the vanityURLCache
     *
     * @param hostId The current host Id
     * @param uri The current URI
     * @param languageId The current language Id
     */
    private void add404URIToCache(String hostId, String uri, long languageId) {
        cache404VanityUrl.setLanguageId(languageId);
        cache404VanityUrl.setURI(uri);
        cache404VanityUrl.setSite(hostId);

        VanityUrlServices.getInstance().updateCache(cache404VanityUrl);
    }

    @Override
    public CachedVanityUrl getLiveCachedVanityUrl(final String uri, final Host host,
            final long languageId, final User user) {

        String hostId = (host != null ? host.getIdentifier() : Host.SYSTEM_HOST);

        CachedVanityUrl result = VanityUrlServices.getInstance()
                .getCachedVanityUrlByUri(uri, hostId, languageId);

        if (patternMatches(result, uri)) {
            return result;
        } else {
            //Search for the URI in the vanityURL cached by host and language Ids
            result = searchLiveCachedVanityUrlByHostAndLanguage(uri, hostId, languageId);
        }

        if (result == null) {

            try {
                List<ContentType> vanityUrlContentTypes = APILocator.getContentTypeAPI(user)
                        .findByType(BaseContentType.VANITY_URL);
                if (!vanityUrlContentTypes.isEmpty()
                        && ((VanityUrlContentType) vanityUrlContentTypes.get(0)).fallback()) {

                    //if the fallback is set then is going to try to get it by the default language
                    long defaultLanguageId = APILocator.getLanguageAPI().getDefaultLanguage()
                            .getId();

                    result = VanityUrlServices.getInstance()
                            .getCachedVanityUrlByUri(uri, hostId, defaultLanguageId);

                    if (patternMatches(result, uri)) {
                        return result;
                    } else {
                        //Search for the URI in the vanityURL cached by host and default language Ids
                        result = searchLiveCachedVanityUrlByHostAndLanguage(uri, hostId,
                                defaultLanguageId);
                    }
                }

            } catch (DotDataException | DotSecurityException e) {
                Logger.error(this, "Error searching for Vanity URL by URI", e);
            }

            if (result == null) {
                //Add 404 to cache
                add404URIToCache(hostId, uri, languageId);
                result = VanityUrlServices.getInstance()
                        .getCachedVanityUrlByUri(uri, hostId, languageId);
            }
        }

        return result;
    }

    /**
     * Validate if the CachedVanityUrl pattern matches the uri
     *
     * @param cachedVanityUrl The CachedVanityUrl
     * @param uri The current uri
     * @return true if the CachedVanityUrl match, false if not
     */
    private boolean patternMatches(CachedVanityUrl cachedVanityUrl, String uri) {
        boolean patternMatches = false;
        if (cachedVanityUrl != null) {
            Matcher matcher = cachedVanityUrl.getPattern().matcher(uri);
            patternMatches = matcher.matches();
        }
        return patternMatches;
    }

    /**
     * Search CachedVanity Url checking if the Uri is in the Host Id and language Id cache
     *
     * @param uri The current uri
     * @param hostId the current host Id
     * @param languageId the current language Id
     * @return a CachedVanityUrl object
     */
    private CachedVanityUrl searchLiveCachedVanityUrlByHostAndLanguage(final String uri,
            final String hostId, final long languageId) {
        CachedVanityUrl result = null;

        //Get the list of host cached Vanities URLs
        Set<CachedVanityUrl> cachedVanityUrls = VanityUrlServices.getInstance()
                .getVanityUrlByHostAndLanguage(hostId, languageId);

        if (cachedVanityUrls.isEmpty()) {
            //Initialize the Cached Vanity URL cache if is null
            VanityUrlServices.getInstance().initializeVanityUrlCache(hostId, languageId);
            cachedVanityUrls = VanityUrlServices.getInstance()
                    .getVanityUrlByHostAndLanguage(hostId, languageId);
        }

        //Validates if onw of the host cachedVanityUrls matches the uri
        for (CachedVanityUrl vanity : cachedVanityUrls) {
            if (patternMatches(vanity, uri)) {
                result = vanity;
                break;
            }
        }
        return result;
    }

    @Override
    public void validateVanityUrl(Contentlet contentlet) throws DotContentletValidationException {
        VanityUrl vanityUrl = getVanityUrlFromContentlet(contentlet);
        User user = null;
        try {
            user = APILocator.getUserAPI().loadUserById(contentlet.getModUser());
        } catch (Exception e) {
            Logger.debug(this,e.getMessage(),e);
            try {
                user = APILocator.getUserAPI().getSystemUser();
            } catch (DotDataException e1) {
                Logger.debug(this,e1.getMessage(),e1);
                throw new DotContentletValidationException("User Not Found");
            }
        }

        if (!VanityUrlUtil.isPatternValid(vanityUrl.getURI())) {
            Language l = APILocator.getLanguageAPI().getLanguage(user.getLanguageId());
            String message = APILocator.getLanguageAPI()
                    .getStringKey(l, "message.vanity.url.error.invalidURIPattern");

            throw new DotContentletValidationException(message);
        }

    }

}
