package com.dotcms.vanityurl.filters;

import com.dotcms.http.CircuitBreakerUrl;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.StringPool;
import io.vavr.control.Try;
import java.io.IOException;
import java.util.Optional;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
          
          if (cachedVanity.isPresent()) {
              final VanityUrlResult vanityUrlResult = cachedVanity.get().handle( uri, response);
              final VanityUrlRequestWrapper vanityUrlRequestWrapper = new VanityUrlRequestWrapper(request, vanityUrlResult);
              // If the handler already resolved the requested URI we stop the processing here
              if (handleVanityURLRedirects(vanityUrlRequestWrapper, response, vanityUrlResult)) {
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
 
    
  }

    /**
     * Product of refactoring handling 301 and 302 previously executed by CachedVanityUrl
     *
     * @return weather or not the redirect was handled
     */
  private boolean handleVanityURLRedirects(final VanityUrlRequestWrapper request,
            final HttpServletResponse response, final VanityUrlResult vanityUrlResult) {
        if (!response.isCommitted()) {
            final String uri = vanityUrlResult.getRewrite();
            final String queryString = request.getQueryString();
            final int responseCode = request.getResponseCode();

            final String newUrl = uri + (queryString != null ? StringPool.QUESTION + queryString
                    : StringPool.BLANK);
            if (responseCode == 301 || responseCode == 302) {
                response.setStatus(responseCode);
                response.setHeader("Location", newUrl);
                return true;
            }

            // if the vanity is a proxy request
            if (responseCode == 200 && UtilMethods.isSet(uri) && uri.contains("//")) {
                Try.run(() -> new CircuitBreakerUrl(newUrl).doOut(response)).onFailure(
                        DotRuntimeException::new);
                return true;
            }
        }
        return false;
  }


} // E:O:F:VanityURLFilter.
