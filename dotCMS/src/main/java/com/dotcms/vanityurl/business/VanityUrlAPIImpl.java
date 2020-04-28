package com.dotcms.vanityurl.business;


import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.contenttype.model.type.ImmutableVanityUrlContentType;
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
import com.dotmarketing.util.UUIDGenerator;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.liferay.util.StringPool;
import io.vavr.control.Try;

/**
 * Implementation class for the {@link VanityUrlAPI}.
 *
 * @author oswaldogallango
 * @version 4.2.0
 * @since June 12, 2017
 */
public class VanityUrlAPIImpl implements VanityUrlAPI {

  private final Set<Integer> allowedActions = new ImmutableSet.Builder<Integer>().add(200).add(301).add(302).build();

  private static final String SELECT_LIVE_VANITY_URL_INODES =
      "SELECT cvi.live_inode FROM contentlet c, identifier i, contentlet_version_info cvi, structure s "
          + " where s.structuretype= 7 and c.structure_inode=s.inode "
          + " and cvi.live_inode=c.inode and cvi.identifier = i.id  and i.host_inode = ? and cvi.lang =? ";

  public static final String URL_SUFFIX = "/";
  public static final String LEGACY_CMS_HOME_PAGE = "/cmsHomePage";
  private final ContentletAPI contentletAPI;
  private final VanityUrlCache cache;
  private final LanguageAPI languageAPI;
  private final boolean languageFallback = Config.getBooleanProperty("DEFAULT_VANITY_URL_TO_DEFAULT_LANGUAGE", false) ;

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
   * @param siteId String site id
   * @param languageId Long language id
   * @return A list of VanityURLs
   */
  private void populateVanityURLsCacheBySite(final Host host) {
    if(host==null || host.getIdentifier()==null) {
        throw new DotStateException("Host cannot be null. Got:" + host);
    }
    Logger.info(this.getClass(), "Populating Vanity URLS for :" + host.getHostname()); 
    for (Language lang : languageAPI.getLanguages()) {
      cache.putSiteMappings(host, lang, findInDb(host, lang));
    }
  }

  @Override
  public void populateAllVanityURLsCache() throws DotDataException {
    for (Host host : Try.of(() -> APILocator.getHostAPI().findAllFromDB(APILocator.systemUser(), false)).getOrElse(ImmutableList.of())) {
      populateVanityURLsCacheBySite(host);
    }
    populateVanityURLsCacheBySite(APILocator.getHostAPI().findSystemHost());
  }

  /**
   * Executes a SQL query that will return all the Vanity URLs that belong to a specific Site. This
   * method moved from using the ES index to using a SQL query in order to avoid situations where the
   * index was not fully updated when reading new data.
   *
   * @param siteId The Identifier of the Site whose Vanity URLs will be retrieved.
   * @param languageId The language ID used to created the Vanity URLs.
   * @param includeSystemHost If set to {@code true}, the results will include Vanity URLs that were
   *        created under System Host. Otherwise, set to {@code false}.
   *
   * @return The list of Vanity URLs.
   */
  @Override
  @CloseDBIfOpened
  public List<CachedVanityUrl> findInDb(final Host host, Language lang) {

    try {
      final List<Map<String, Object>> vanityUrls =
          new DotConnect().setSQL(SELECT_LIVE_VANITY_URL_INODES).addParam(host.getIdentifier()).addParam(lang.getId()).loadObjectResults();
      final List<String> vanityUrlInodes =
          vanityUrls.stream().map(vanity -> vanity.get("live_inode").toString()).collect(Collectors.toList());
      final List<Contentlet> contentlets = this.contentletAPI.findContentlets(vanityUrlInodes);

      return contentlets.stream().map(c -> new CachedVanityUrl(this.fromContentlet(c))).collect(Collectors.toList());

    } catch (final Exception e) {
      Logger.error(this,
          String.format("An error occurred when retrieving Vanity URLs: siteId=[%s], " + "languageId=[%s], includeSystemHost=[%s]",
              host.getIdentifier(), lang.getId()),
          e);
      throw new DotStateException(e);
    }

  }


  
  
  
  private List<CachedVanityUrl> load(Host host, Language lang){
    List<CachedVanityUrl> cachedVanities = cache.getSiteMappings(host, lang);
    if(cachedVanities==null) {
      synchronized (VanityUrlAPI.class) {
        cachedVanities = cache.getSiteMappings(host, lang);
        if(cachedVanities==null) {
          cachedVanities=findInDb(host, lang);
          cache.putSiteMappings(host, lang, findInDb(host, lang));
        }
      }
    }
    return cachedVanities;

  }
  

  
  @CloseDBIfOpened
  @Override
  public Optional<CachedVanityUrl> resolveVanityUrl(final String url, final Host host, final Language lang) {

    // 404 short circuit
    Optional<CachedVanityUrl> shortCircuit = cache.getDirectMapping(url, host, lang);
    if(shortCircuit!=null) {
        return shortCircuit;
    }


    // tries specific site first
    Optional<CachedVanityUrl> matched =load(host, lang).parallelStream().filter(vc -> vc.url.equalsIgnoreCase(url) || vc.pattern.matcher(url).find()).findFirst();

    
    // try language fallback
    if (!matched.isPresent()  && !languageAPI.getDefaultLanguage().equals(lang) && languageFallback ) {
      matched = resolveVanityUrl(url, host, languageAPI.getDefaultLanguage());
    }
    
    // tries SYSTEM_HOST
    if (!matched.isPresent() && !APILocator.systemHost().equals(host)) {
      matched = load(APILocator.systemHost(), lang).parallelStream().filter(vc -> vc.pattern.matcher(url).find()).findFirst();
    }
    
    // if this is the /cmsHomePage vanity
    if (!matched.isPresent() && StringPool.FORWARD_SLASH.equals(url)) {
        matched = resolveVanityUrl(LEGACY_CMS_HOME_PAGE, host, lang);
    }
    
    
    // whatever we have, stick it into the cache so it can be remembered
    cache.putDirectMapping(host, lang, url, matched);


    return matched;
  } // isVanityUrl

  
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
      throw new DotStateException("Contentlet is null");
    }

    if (!contentlet.isVanityUrl()) {
      throw new DotStateException("Contentlet : " + contentlet.getInode() + " is not a Vanity Url");
    }

    if (contentlet instanceof VanityUrl) {
      return (VanityUrl) contentlet;
    }

    final DefaultVanityUrl vanityUrl = new DefaultVanityUrl();
    vanityUrl.setContentTypeId(contentlet.getContentTypeId());
    try {
      this.contentletAPI.copyProperties(vanityUrl, contentlet.getMap());
    } catch (Exception e) {
      throw new DotStateException("Vanity Url Copy Failed", e);
    }
    vanityUrl.setHost(contentlet.getHost());
    vanityUrl.setFolder(contentlet.getFolder());
    CacheLocator.getContentletCache().add(vanityUrl);
    return vanityUrl;
  } // getVanityUrlFromContentlet.

  
  @CloseDBIfOpened
  @Override
  public void validateVanityUrl(final Contentlet contentlet) {

    Language language = Try.of(()-> WebAPILocator.getLanguageWebAPI().getLanguage(HttpServletRequestThreadLocal.INSTANCE.getRequest())).getOrElse(APILocator.getLanguageAPI().getDefaultLanguage());

    // check fields
    checkMissingField(contentlet, language, VanityUrlContentType.ACTION_FIELD_VAR);
    checkMissingField(contentlet, language, VanityUrlContentType.URI_FIELD_VAR);

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

  private void checkMissingField(final Contentlet contentlet, final Language language, final String fieldName) {

    final String identifier = contentlet.getIdentifier();

    if (!contentlet.getMap().containsKey(fieldName)) {

      throw new DotContentletValidationException(
          MessageFormat.format(this.languageAPI.getStringKey(language, "missing.field"), fieldName, identifier));
    }
  }

}
