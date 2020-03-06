
package com.dotcms.visitor.filter.servlet;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;

import com.dotcms.vanityurl.model.CachedVanityUrl;

import com.dotcms.visitor.filter.logger.VisitorLogger;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.LanguageWebAPI;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.filters.CMSUrlUtil;

import com.dotmarketing.logConsole.model.LogMapper;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Logger;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Optional;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;

public class VisitorFilter implements Filter {


    private final CMSUrlUtil urlUtil;

    private final LanguageWebAPI languageWebAPI;
    private final UserWebAPI userWebAPI;
    private final static String CMS_HOME_PAGE = "/cmsHomePage";
    public  final static  String VANITY_URL_ATTRIBUTE="VANITY_URL_ATTRIBUTE";
    public  final static  String DOTPAGE_PROCESSING_TIME="DOTPAGE_PROCESSING_TIME";



    public VisitorFilter() {
        this( CMSUrlUtil.getInstance(), WebAPILocator.getHostWebAPI(),
                WebAPILocator.getLanguageWebAPI(), WebAPILocator.getUserWebAPI());
    }

    @VisibleForTesting
    protected VisitorFilter( final CMSUrlUtil urlUtil,
            final HostWebAPI hostWebAPI, final LanguageWebAPI languageWebAPI, final UserWebAPI userWebAPI) {
        
        this.urlUtil = urlUtil;
        this.languageWebAPI = languageWebAPI;
        this.userWebAPI = userWebAPI;
    }

    public void init(FilterConfig config) throws ServletException {
        Logger.info(this.getClass(), "VisitorLogger Filter Started");
    }

    public void doFilter(final ServletRequest req, final ServletResponse res, final FilterChain chain)
            throws IOException, ServletException {

        if(LicenseUtil.getLevel() >= LicenseLevel.STANDARD.level) {
            final boolean isNewConnection = !DbConnectionFactory.connectionExists();
            final HttpServletRequest request = (HttpServletRequest) req;
            final HttpServletResponse response = (HttpServletResponse) res;
            try {
                long startTime = System.currentTimeMillis();
                chain.doFilter(req, res);
                setVanityAsAttribute(request);
                request.setAttribute(DOTPAGE_PROCESSING_TIME,
                        System.currentTimeMillis() - startTime);
                VisitorLogger.log(request, response);
            } catch (Exception e) {
                Logger.error(this.getClass(), e.getMessage(), e);
                return;
            } finally {
                if (isNewConnection) {
                    DbConnectionFactory.closeSilently();
                }
            }
        }else{
            chain.doFilter(req, res);
        }
    }

    public void destroy() {
        Logger.info(this.getClass(), "VisitorLogger Filter Destroyed");
    }


    private void setVanityAsAttribute(HttpServletRequest request)
            throws UnsupportedEncodingException, DotDataException, PortalException, SystemException, DotSecurityException {
        // Get the URI from the request and check for possible XSS hacks
        final String uri = this.urlUtil.getURIFromRequest(request);
        if (this.urlUtil.isVanityUrlFiltered(uri)) {
            return;
        }
        // Getting the site form the request
        final Host host = WebAPILocator.getHostWebAPI().getCurrentHost(request);
        final Language language = this.languageWebAPI.getLanguage(request);

        Optional<CachedVanityUrl> vanityUrl = APILocator.getVanityUrlAPI().resolveVanityUrl(uri, host, language);
        if(vanityUrl.isPresent()) {
            request.setAttribute(VANITY_URL_ATTRIBUTE, vanityUrl.get().getVanityUrlId());
        }
            
        
    }

}
