package com.dotcms.vanityurl.business;

import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.VanityUrlContentType;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
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
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Logger;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.regex.Matcher;

import org.elasticsearch.indices.IndexMissingException;

/**
 * Implementation class for the {@link VanityUrlAPI}.
 *
 * @author oswaldogallango
 * @version 4.2.0
 * @since June 12, 2017
 */
public class VanityUrlAPIImpl implements VanityUrlAPI {

    private final ContentletAPI contentletAPI;
    private final VanityUrlServices vanityUrlServices;

    private static final String GET_VANITY_URL_BASE_TYPE =
            "+baseType:" + BaseContentType.VANITY_URL.getType();
    private static final String GET_ACTIVE_VANITY_URL =
            GET_VANITY_URL_BASE_TYPE + " +live:true +deleted:false";
    private static final String GET_VANITY_URL_LANGUAGE_ID = " +languageId:";
    private static final int CODE_404_VALUE = 404;

    public VanityUrlAPIImpl() {
        this(APILocator.getContentletAPI(),
                VanityUrlServices.getInstance());
    }

    @VisibleForTesting
    public VanityUrlAPIImpl(final ContentletAPI contentletAPI,
                            final VanityUrlServices vanityUrlServices) {

        this.contentletAPI = contentletAPI;
        this.vanityUrlServices = vanityUrlServices;
    }

    @Override
    public List<VanityUrl> getActiveVanityUrls(final User user) {
        return searchAndPopulate(GET_ACTIVE_VANITY_URL, user);
    }

    @Override
    public List<VanityUrl> getActiveVanityUrlsBySiteAndLanguage(final String siteId,
                                                                final long languageId, final User user) {

        final String luceneQuery = GET_ACTIVE_VANITY_URL + " +conHost:" + siteId
                + GET_VANITY_URL_LANGUAGE_ID
                + languageId;
        return searchAndPopulate(luceneQuery, user);
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
            contentletAPI.copyProperties(vanityUrl, con.getMap());
        } catch (Exception e) {
            throw new DotStateException("Vanity Url Copy Failed", e);
        }
        vanityUrl.setHost(con.getHost());
        vanityUrl.setFolder(con.getFolder());

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
                vanityUrlServices.updateCache(vanityUrl);
            } else {
                vanityUrlServices.invalidateVanityUrl(vanityUrl);
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
     * @param siteId The current site Id
     * @param uri The current URI
     * @param languageId The current language Id
     */
    private void add404URIToCache(String siteId, String uri, long languageId) {

        VanityUrl cache404VanityUrl = new DefaultVanityUrl();
        cache404VanityUrl.setInode(VanityUrlAPI.CACHE_404_VANITY_URL);
        cache404VanityUrl.setIdentifier(VanityUrlAPI.CACHE_404_VANITY_URL);
        cache404VanityUrl.setAction(CODE_404_VALUE);
        cache404VanityUrl.setLanguageId(languageId);
        cache404VanityUrl.setURI(uri);
        cache404VanityUrl.setSite(siteId);

        vanityUrlServices.updateCache(cache404VanityUrl);
    }

    @Override
    public CachedVanityUrl getLiveCachedVanityUrl(final String uri, final Host site,
                                                  final long languageId, final User user) {

        String siteId = (site != null ? site.getIdentifier() : Host.SYSTEM_HOST);

        //First lets try with the cache
        CachedVanityUrl result = vanityUrlServices
                .getCachedVanityUrlByUri(uri, siteId, languageId);

        if (patternMatches(result, uri)) {
            return result;
        } else {
            //Search for the URI in the vanityURL cached by site and language Ids
            result = searchLiveCachedVanityUrlBySiteAndLanguage(uri, siteId, languageId);
        }

        if (result == null) {

            try {

                //Search for the list of ContentTypes of type VanityURL
                List<ContentType> vanityUrlContentTypes = APILocator.getContentTypeAPI(user)
                        .findByType(BaseContentType.VANITY_URL);

                //Verify if this Content Type has a Multilinguable fallback
                if (!vanityUrlContentTypes.isEmpty()
                        && ((VanityUrlContentType) vanityUrlContentTypes.get(0)).fallback()) {

                    //if the fallback is set then is going to try to get it by the default language
                    long defaultLanguageId = APILocator.getLanguageAPI().getDefaultLanguage()
                            .getId();

                    result = vanityUrlServices
                            .getCachedVanityUrlByUri(uri, siteId, defaultLanguageId);

                    if (patternMatches(result, uri)) {
                        return result;
                    } else {
                        //Search for the URI in the vanityURL cached by site and default language Ids
                        result = searchLiveCachedVanityUrlBySiteAndLanguage(uri, siteId,
                                defaultLanguageId);
                    }
                }

            } catch (DotDataException | DotSecurityException e) {
                Logger.error(this, "Error searching for Vanity URL by URI", e);
            }

            if (result == null) {
                //Add 404 to cache
                add404URIToCache(siteId, uri, languageId);
                result = vanityUrlServices
                        .getCachedVanityUrlByUri(uri, siteId, languageId);
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
     * Search CachedVanity Url checking if the Uri is in the Site Id and language Id cache
     *
     * @param uri The current uri
     * @param siteId the current site Id
     * @param languageId the current language Id
     * @return a CachedVanityUrl object
     */
    private CachedVanityUrl searchLiveCachedVanityUrlBySiteAndLanguage(final String uri,
                                                                       final String siteId, final long languageId) {

        CachedVanityUrl result = null;

        //Get the list of site cached Vanities URLs
        Set<CachedVanityUrl> cachedVanityUrls = vanityUrlServices
                .getVanityUrlBySiteAndLanguage(siteId, languageId);

        if (cachedVanityUrls.isEmpty()) {

            synchronized (VanityUrlAPIImpl.class) {

                cachedVanityUrls = vanityUrlServices
                        .getVanityUrlBySiteAndLanguage(siteId, languageId);

                if (cachedVanityUrls.isEmpty()) {

                    //Initialize the Cached Vanity URL cache if is null
                    vanityUrlServices.initializeVanityUrlCache(siteId, languageId);

                    //Get the list of site cached Vanities URLs
                    cachedVanityUrls = vanityUrlServices
                            .getVanityUrlBySiteAndLanguage(siteId, languageId);
                }
            }
        }

        //Validates if onw of the site cachedVanityUrls matches the uri
        for (CachedVanityUrl vanity : cachedVanityUrls) {
            if (patternMatches(vanity, uri)) {
                result = vanity;
                break;
            }
        }

        /*
        At this point we already found and saved in cache the VanityURL that matches with the given
        URL but adding a new VanityURL to the cache for the completed requested URL
        and not just the regex used can save us time.
         */
        if (null != result) {
            if (!result.getUrl().equals(uri)) {
                CachedVanityUrl cachedVanityUrl = new CachedVanityUrl(result, uri);
                vanityUrlServices.updateCache(cachedVanityUrl);
            }
        }

        return result;
    }

    /**
     * Using a given lucene query this method searches for VanityURLs, each VanityURL found is
     * added into the cache.
     * <br>
     * Note this method does not uses cache, always does the ES search, the intention of this method
     * is mainly to populate the cache with the found data.
     *
     * @param luceneQuery query for the ES search
     * @param user to use in the ES search
     * @return A list of VanityURLs
     */
	private List<VanityUrl> searchAndPopulate(final String luceneQuery, final User user) {

		final ImmutableList.Builder<VanityUrl> results = new ImmutableList.Builder();
		final PriorityQueue<VanityUrl> vanityUrls;
		List<VanityUrl> vanityUrlsToReturn = Collections.emptyList();

		try {
			final List<Contentlet> contentResults = contentletAPI.search(luceneQuery, 0, 0, StringPool.BLANK, user,
					false);

			// Verify if we have something to process
			if (null == contentResults || contentResults.isEmpty()) {
				return results.build();
			}

			vanityUrls = new PriorityQueue<>(contentResults.size(),
					(vanity1, vanity2) -> vanity1.getOrder() - vanity2.getOrder());

			contentResults.stream().forEach((Contentlet con) -> {
				VanityUrl vanityUrl = getVanityUrlFromContentlet(con);
				addToVanityURLCache(vanityUrl);
				vanityUrls.offer(vanityUrl);
			});

			vanityUrlsToReturn = results.addAll(vanityUrls).build();
		}
		catch (IndexMissingException e){
			/*
			 * We catch this exception in order to avoid to stop the
			 * initialization of dotCMS if for some reason at this point we
			 * don't have indexes.
			 */
			Logger.error(this, "Error when initializing Vanity URLs, no index found ", e);
		}
		catch (DotDataException | DotSecurityException e ){
			Logger.error(this, "Error searching for active Vanity URLs [" + luceneQuery + "]", e);
		}
		catch (Exception e){
			if ( e.getCause() instanceof IndexMissingException){
				/*
				 * We catch this exception in order to avoid to stop the
				 * initialization of dotCMS if for some reason at this point we
				 * don't have indexes.
				 */
				Logger.error(this, "Error when initializing Vanity URLs, no index found ", e);
			}else{
				throw new DotRuntimeException("Error searching and populating the Vanity URL Cache", e);
			}
		}


		return vanityUrlsToReturn;
	}

    @Override
    public void validateVanityUrl(Contentlet contentlet) {

        VanityUrl vanityUrl = getVanityUrlFromContentlet(contentlet);
        User user;
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

        if (!VanityUrlUtil.isValidRegex(vanityUrl.getURI())) {
            Language l = APILocator.getLanguageAPI().getLanguage(user.getLanguageId());
            String message = APILocator.getLanguageAPI()
                    .getStringKey(l, "message.vanity.url.error.invalidURIPattern");

            throw new DotContentletValidationException(message);
        }

    }

}