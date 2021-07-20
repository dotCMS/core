package com.dotcms.vanityurl.business;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.VanityUrlContentType;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.vanityurl.cache.VanityUrlCache;
import com.dotcms.vanityurl.model.CachedVanityUrl;
import com.dotcms.vanityurl.model.DefaultVanityUrl;
import com.dotcms.vanityurl.model.VanityUrl;
import com.dotcms.vanityurl.util.VanityUrlUtil;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletValidationException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.liferay.util.StringPool;
import io.vavr.control.Try;
import org.apache.http.HttpStatus;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation class for the {@link VanityUrlAPI}.
 *
 * @author oswaldogallango
 * @version 4.2.0
 * @since June 12, 2017
 */
public class VanityUrlAPIImpl implements VanityUrlAPI {

  private final Set<Integer> allowedActions = new ImmutableSet.Builder<Integer>()
          .add(HttpStatus.SC_OK)
          .add(HttpStatus.SC_MOVED_PERMANENTLY)
          .add(HttpStatus.SC_MOVED_TEMPORARILY).build();

  private static final String SELECT_LIVE_VANITY_URL_INODES =
      "SELECT cvi.live_inode FROM contentlet c, identifier i, contentlet_version_info cvi, structure s "
          + " where s.structuretype = ? and c.structure_inode = s.inode "
          + " and cvi.live_inode=c.inode and cvi.identifier = i.id  and i.host_inode = ? and cvi.lang =? ";

  public static final String   LEGACY_CMS_HOME_PAGE = "/cmsHomePage";
  private final ContentletAPI  contentletAPI;
  private final VanityUrlCache cache;
  private final LanguageAPI    languageAPI;
  private final boolean        languageFallback = Config.getBooleanProperty("DEFAULT_VANITY_URL_TO_DEFAULT_LANGUAGE", false) ;

  public VanityUrlAPIImpl()  {
    this(APILocator.getContentletAPI(),
        APILocator.getLanguageAPI(), 
        APILocator.getUserAPI(),
        CacheLocator.getVanityURLCache());
  }

  @VisibleForTesting
  public VanityUrlAPIImpl(final ContentletAPI contentletAPI, 
      final LanguageAPI languageAPI,
      final UserAPI userAPI,
      final VanityUrlCache cache) {
    this.contentletAPI = contentletAPI;
    this.languageAPI = languageAPI;
    this.cache = cache;
  }

  /**
   * Searches for all Vanity URLs for a given Site in the system. It goes directly to the database in
   * order to avoid retrieving data that has not been updated in the ES index yet. This initialization
   * routine will also add Vanity URLs located under System Host.
   *
   * @param site Site to populate vanity urls
   */
  private void populateVanityURLsCacheBySite(final Host site) {
    if(site==null || site.getIdentifier()==null) {
        throw new DotStateException("Site cannot be null. Got: " + site);
    }
    Logger.info(this.getClass(), String.format("Populating Vanity URLs for Site '%s'", site.getHostname()));
    for (final Language language : languageAPI.getLanguages()) {
      cache.putSiteMappings(site, language, findInDb(site, language));
    }
  }

  @Override
  public void populateAllVanityURLsCache() throws DotDataException {
    for (final Host site : Try.of(() -> APILocator.getHostAPI().findAllFromDB(APILocator.systemUser(), false)).getOrElse(ImmutableList.of())) {
      populateVanityURLsCacheBySite(site);
    }
    populateVanityURLsCacheBySite(APILocator.getHostAPI().findSystemHost());
  }

  /**
   * Executes a SQL query that will return all the Vanity URLs that belong to a specific Site. This
   * method moved from using the ES index to using a SQL query in order to avoid situations where the
   * index was not fully updated when reading new data.
   *
   * @param site The Site whose Vanity URLs will be retrieved.
   * @param language The language used to created the Vanity URLs.
   *
   * @return The list of Vanity URLs.
   */
  @Override
  @CloseDBIfOpened
  public List<CachedVanityUrl> findInDb(final Host site, final Language language) {

    try {
      final List<Map<String, Object>> vanityUrls = new DotConnect().setSQL(SELECT_LIVE_VANITY_URL_INODES)
              .addParam(BaseContentType.VANITY_URL.getType())
              .addParam(site.getIdentifier())
              .addParam(language.getId()).loadObjectResults();
      final List<String> vanityUrlInodes =
          vanityUrls.stream().map(vanity -> vanity.get("live_inode").toString()).collect(Collectors.toList());
      final List<Contentlet> contentlets = this.contentletAPI.findContentlets(vanityUrlInodes);

      return contentlets.stream().map(contentlet -> new CachedVanityUrl(this.fromContentlet(contentlet))).sorted().collect(Collectors.toList());

    } catch (final Exception e) {
      Logger.error(this,
          String.format("An error occurred when retrieving Vanity URLs: siteId=[%s], languageId=[%s]",
              site.getIdentifier(), language.getId()),
          e);
      throw new DotStateException(e);
    }

  }

  private List<CachedVanityUrl> load(final Host site, final Language language) {

    List<CachedVanityUrl> cachedVanities = cache.getSiteMappings(site, language);
    if(cachedVanities == null) {
      synchronized (VanityUrlAPI.class) {
        cachedVanities = cache.getSiteMappings(site, language);
        if(cachedVanities==null) {
          cachedVanities = findInDb(site, language);
          cache.putSiteMappings(site, language, cachedVanities);
        }
      }
    }

    return cachedVanities;
  }

  @CloseDBIfOpened
  @Override
  public Optional<CachedVanityUrl> resolveVanityUrl(final String url, final Host site, final Language language) {


    // 404 short circuit
    final Optional<CachedVanityUrl> shortCircuit = cache.getDirectMapping(url, site, language);
    if(shortCircuit!=null) {
        return shortCircuit;
    }

    // tries specific site, language and url
    Optional<CachedVanityUrl> matched = load(site, language).stream()
            .filter(cachedVanityUrl ->
                    cachedVanityUrl.url.equalsIgnoreCase(url)).findFirst();

    // tries specific site, language and pattern
    if(!matched.isPresent()) {
      matched = load(site, language).stream()
              .filter(cachedVanityUrl ->
                      cachedVanityUrl.pattern.matcher(url).matches()).findFirst();
    }

    
    // try language fallback
    if (!matched.isPresent()  && !languageAPI.getDefaultLanguage().equals(language) && languageFallback) {

      matched = resolveVanityUrl(url, site, languageAPI.getDefaultLanguage());
    }
    
    // tries SYSTEM_HOST
    if (!matched.isPresent() && !APILocator.systemHost().equals(site)) {

        matched = resolveVanityUrl(url, APILocator.systemHost(), language);
    }
    
    // if this is the /cmsHomePage vanity
    if (!matched.isPresent() && StringPool.FORWARD_SLASH.equals(url)) {

        matched = resolveVanityUrl(LEGACY_CMS_HOME_PAGE, site, language);
    }
    
    
    // whatever we have, stick it into the cache so it can be remembered
    cache.putDirectMapping(site, language, url, matched);


    return matched;
  } // resolveVanityUrl

  
  @CloseDBIfOpened
  @Override
  public void invalidateVanityUrl(final Contentlet vanityURL) {
    if (vanityURL == null || !vanityURL.isVanityUrl()) {
     return;
    }
    cache.remove(vanityURL);
  
  }
  @CloseDBIfOpened
  @Override
  public VanityUrl fromContentlet(final Contentlet contentlet) {

    if (contentlet == null) {
      throw new DotStateException("Contentlet cannot be null");
    }

    if (!contentlet.isVanityUrl()) {
      throw new DotStateException(String.format("Contentlet with Inode '%s' is not a Vanity Url", contentlet.getInode
              ()));
    }

    if (contentlet instanceof VanityUrl) {
      return (VanityUrl) contentlet;
    }
    validateVanityUrl(contentlet);
    final DefaultVanityUrl vanityUrl = new DefaultVanityUrl();
    vanityUrl.setContentTypeId(contentlet.getContentTypeId());
    try {
      this.contentletAPI.copyProperties(vanityUrl, contentlet.getMap());
    } catch (final Exception e) {
        throw new DotStateException(String.format("Failed to copy properties for Vanity Url with ID '%s': %s",
                contentlet.getIdentifier(), e.getMessage()), e);
    }
    vanityUrl.setHost(contentlet.getHost());
    vanityUrl.setFolder(contentlet.getFolder());
    CacheLocator.getContentletCache().add(vanityUrl);
    return vanityUrl;
  } // fromContentlet.

  
  @CloseDBIfOpened
  @Override
  public void validateVanityUrl(final Contentlet contentlet) {

    final Language language = Try.of(()-> WebAPILocator.getLanguageWebAPI().getLanguage(HttpServletRequestThreadLocal.INSTANCE.getRequest())).getOrElse(APILocator.getLanguageAPI().getDefaultLanguage());

    // check fields
    checkMissingField(contentlet, language, VanityUrlContentType.ACTION_FIELD_VAR);
    checkMissingField(contentlet, language, VanityUrlContentType.URI_FIELD_VAR);
    checkMissingField(contentlet, language, VanityUrlContentType.FORWARD_TO_FIELD_VAR);

    final Integer action = (int) contentlet.getLongProperty(VanityUrlContentType.ACTION_FIELD_VAR);
    final String uri = contentlet.getStringProperty(VanityUrlContentType.URI_FIELD_VAR);

    if (!this.allowedActions.contains(action)) {
      throwContentletValidationError(language, "message.vanity.url.error.invalidAction");
    }

    if (!VanityUrlUtil.isValidRegex(uri)) {
      throwContentletValidationError(language, "message.vanity.url.error.invalidURIPattern");
    }
  } // validateVanityUrl.

  private void throwContentletValidationError(final Language language, final String key) {
    final String message = this.languageAPI.getStringKey(language, key);

    throw new DotContentletValidationException(message);
  }

    /**
     * Checks that the specified {@code fieldName} property is both present and has a non-empty value in the {@link
     * Contentlet} object.
     *
     * @param contentlet The {@link Contentlet} representation of the Vanity URL.
     * @param language   The {@link Language} of the Vanity URL.
     * @param fieldName  The name of the field that will be verified.
     *
     * @throws DotContentletValidationException The specified field name was either not found, or was null/empty.
     */
  private void checkMissingField(final Contentlet contentlet, final Language language, final String fieldName) {

    final String identifier = contentlet.getIdentifier();

    if (!contentlet.getMap().containsKey(fieldName) || !UtilMethods.isSet(contentlet.get(fieldName))) {

      throw new DotContentletValidationException(
          MessageFormat.format(this.languageAPI.getStringKey(language, "missing.field"), fieldName, identifier));
    }
  }

}
