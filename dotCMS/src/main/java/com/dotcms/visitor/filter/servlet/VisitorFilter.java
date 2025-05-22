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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.glassfish.hk2.api.MultiException;

public class VisitorFilter implements Filter {

    private static final String SERVICE_LOCATOR_SHUTDOWN_PATTERN = 
            ".*DotServiceLocatorImpl\\(__HK2_Generated_\\d+,\\d+,\\d+\\) has been shut down.*";
    private static final Pattern SERVICE_LOCATOR_PATTERN = Pattern.compile(SERVICE_LOCATOR_SHUTDOWN_PATTERN);

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

    /**
     * Check if the exception is a service locator shutdown exception
     * 
     * @param exception The exception to check
     * @return true if the exception is a service locator shutdown
     */
    private boolean isServiceLocatorShutdownException(Throwable exception) {
        if (exception == null) {
            return false;
        }
        
        // Check if this is a MultiException with a single cause
        if (exception instanceof MultiException) {
            MultiException multiException = (MultiException) exception;
            if (multiException.getErrors() != null && multiException.getErrors().size() == 1) {
                return isServiceLocatorShutdownException(multiException.getErrors().get(0));
            }
        }
        
        // Check if this is an IllegalStateException with the right message
        if (exception instanceof IllegalStateException) {
            String message = exception.getMessage();
            if (message != null) {
                Matcher matcher = SERVICE_LOCATOR_PATTERN.matcher(message);
                return matcher.matches();
            }
        }
        
        // Check the cause recursively
        return isServiceLocatorShutdownException(exception.getCause());
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
                // Check if this is a service locator shutdown exception, which we can safely ignore during undeployment
                if (isServiceLocatorShutdownException(e)) {
                    Logger.info(this.getClass(), 
                        "ServiceLocator shutdown exception caught and handled during OSGI plugin undeploy: " + e.getMessage());
                    return;
                }
                
                // For all other exceptions, log and continue with normal error handling
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
            request.setAttribute(VANITY_URL_ATTRIBUTE, vanityUrl.get().vanityUrlId);
        }
            
        
    }

}
