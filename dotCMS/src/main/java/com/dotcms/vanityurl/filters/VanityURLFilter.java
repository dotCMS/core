package com.dotcms.vanityurl.filters;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.vanityurl.business.VanityUrlAPI;
import com.dotcms.vanityurl.model.CachedVanityUrl;
import com.dotcms.vanityurl.model.VanityUrlResult;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.LanguageWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.filters.CMSUrlUtil;
import com.dotmarketing.filters.Constants;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Config;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

import static com.dotmarketing.filters.Constants.VANITY_URL_OBJECT;

/**
 * This Filter handles the vanity url logic
 * 
 * @author Jonathan Gamba 7/27/17
 */
// todo: change this to an interceptor
public class VanityURLFilter implements Filter {

  private final CMSUrlUtil urlUtil;
  private final HostWebAPI hostWebAPI;
  private final LanguageWebAPI languageWebAPI;
  private final VanityUrlAPI vanityApi;
  private final boolean addVanityHeader;

  public VanityURLFilter() {

    this(CMSUrlUtil.getInstance(), WebAPILocator.getHostWebAPI(), WebAPILocator.getLanguageWebAPI(),
       APILocator.getVanityUrlAPI());
  }

  @VisibleForTesting
  protected VanityURLFilter( final CMSUrlUtil urlUtil, final HostWebAPI hostWebAPI,
      final LanguageWebAPI languageWebAPI, final VanityUrlAPI vanityApi) {
    this.vanityApi = vanityApi;

    this.urlUtil = urlUtil;
    this.hostWebAPI = hostWebAPI;
    this.languageWebAPI = languageWebAPI;
    this.addVanityHeader=Config.getBooleanProperty("VANITY_URL_INCLUDE_HEADER", true);
  }
  @Override
  public void init(FilterConfig filterConfig) throws ServletException {}

  @Override
  public void doFilter(final ServletRequest req, final ServletResponse res, final FilterChain filterChain)
                  throws IOException, ServletException {

      final HttpServletRequest request = (HttpServletRequest) req;
      final HttpServletResponse response = (HttpServletResponse) res;

      // Get the URI from the request and check for possible XSS hacks
      final String uri = this.urlUtil.getURIFromRequest(request);
      
      final boolean isFiltered = this.urlUtil.isVanityUrlFiltered(uri);


      // do not run again if the filter has been run
      if (!isFiltered && request.getAttribute(Constants.VANITY_URL_HAS_RUN) == null) {
          request.setAttribute(Constants.VANITY_URL_HAS_RUN, true);
          // Getting the site form the request
          final Host host = hostWebAPI.getCurrentHostNoThrow(request);
          final Language language = this.languageWebAPI.getLanguage(request);
          final Optional<CachedVanityUrl> cachedVanity = vanityApi.resolveVanityUrl(uri, host, language);
          
          if (null != cachedVanity
                  && cachedVanity.isPresent()
                  && null != cachedVanity.get()
                  // checks if the current destiny is not exactly the forward of the vanity
                  // we do this to avoid infinite loop
                 && !vanityApi.isSelfReferenced(cachedVanity.get(), uri)) {

              request.setAttribute(VANITY_URL_OBJECT, cachedVanity.get());
              if(addVanityHeader) {
                  response.setHeader(VanityUrlAPI.VANITY_URL_RESPONSE_HEADER, cachedVanity.get().vanityUrlId);
              }
              final VanityUrlResult vanityUrlResult = cachedVanity.get().handle(uri);
              final VanityUrlRequestWrapper vanityUrlRequestWrapper = new VanityUrlRequestWrapper(request, vanityUrlResult);
              // If the handler already resolved the requested URI we stop the processing here
              if (this.vanityApi.handleVanityURLRedirects(vanityUrlRequestWrapper, response, vanityUrlResult)) {
                return;
              }
              filterChain.doFilter(vanityUrlRequestWrapper, response);
              return;
          }

      }

      filterChain.doFilter(request, response);
  } // doFilter.

  @Override
  public void destroy() {
    // Not implemented
  }

} // E:O:F:VanityURLFilter.
