package com.dotcms.vanityurl.filters;

import static com.dotmarketing.filters.Constants.CMS_FILTER_QUERY_STRING_OVERRIDE;
import static com.dotmarketing.filters.Constants.CMS_FILTER_URI_OVERRIDE;

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
import com.dotcms.vanityurl.handler.DefaultVanityUrlHandler;
import com.dotcms.vanityurl.handler.VanityUrlHandler;
import com.dotcms.vanityurl.model.CachedVanityUrl;
import com.dotcms.vanityurl.model.VanityUrlResult;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.LanguageWebAPI;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.filters.CMSUrlUtil;
import com.dotmarketing.portlets.languagesmanager.model.Language;

/**
 * This Filter handles the vanity url logic
 * 
 * @author Jonathan Gamba 7/27/17
 */
// todo: change this to an interceptor
public class VanityURLFilter implements Filter {

  private final VanityUrlHandler vanityUrlHandler;
  private final CMSUrlUtil urlUtil;
  private final HostWebAPI hostWebAPI;
  private final LanguageWebAPI languageWebAPI;
  private final VanityUrlAPI vanityApi;

  public VanityURLFilter() {

    this(new DefaultVanityUrlHandler(), CMSUrlUtil.getInstance(), WebAPILocator.getHostWebAPI(), WebAPILocator.getLanguageWebAPI(),
       APILocator.getVanityUrlAPI());
  }

  @VisibleForTesting
  protected VanityURLFilter(final VanityUrlHandler vanityUrlHandler, final CMSUrlUtil urlUtil, final HostWebAPI hostWebAPI,
      final LanguageWebAPI languageWebAPI, final VanityUrlAPI vanityApi) {
    this.vanityApi = vanityApi;
    this.vanityUrlHandler = vanityUrlHandler;
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
    if (!isFiltered) {
      // Getting the site form the request
      final Host host = hostWebAPI.getCurrentHostNoThrow(request);
      final Language language = this.languageWebAPI.getLanguage(request);
      final Optional<CachedVanityUrl> cachedVanity = vanityApi.resolveVanityUrl(uri, host, language);
      
      if (cachedVanity.isPresent()) {
        final VanityUrlResult vanityUrlResult = vanityUrlHandler.handle(cachedVanity, uri, response);

        // If the handler already resolved the requested URI we stop the processing here
        if (vanityUrlResult.isResolved()) {
          return;
        }

        /*
         * If the VanityURL has a query string we need to add it to the request in order to override in the
         * other filters.
         */
        if (vanityUrlResult.getQueryString() != null) {
          request.setAttribute(CMS_FILTER_QUERY_STRING_OVERRIDE, vanityUrlResult.getQueryString());
        }

        /*
         * Set into the request the VanityURL we need to use to rewrite the current URI
         */
        request.setAttribute(CMS_FILTER_URI_OVERRIDE, vanityUrlResult.getRewrite());
      }
    }

    filterChain.doFilter(request, response);
  } // doFilter.

  @Override
  public void destroy() {
 
    
  }

} // E:O:F:VanityURLFilter.
