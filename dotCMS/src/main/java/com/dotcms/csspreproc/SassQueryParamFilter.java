package com.dotcms.csspreproc;

import com.dotcms.uuid.shorty.ShortType;
import com.dotcms.uuid.shorty.ShortyId;
import com.dotcms.uuid.shorty.ShortyIdAPI;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

/**
 * This filter intercepts requests with the dotsass=true parameter and forwards them
 * to the SASS preprocessor servlet. It supports two URL patterns:
 * 1. /path/to/file.scss?dotsass=true - Compiles the SCSS file directly
 * 2. /dA/{identifier}?dotsass=true - Compiles the SCSS file identified by the shorty ID
 *
 * @author Travis Caruth
 */
public class SassQueryParamFilter implements Filter {

    private final ShortyIdAPI shortyIdAPI = APILocator.getShortyAPI();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // No initialization needed
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpReq = (HttpServletRequest) request;
        HttpServletResponse httpResp = (HttpServletResponse) response;
        
        // Check if the dotsass parameter is present and true
        String dotsassParam = httpReq.getParameter("dotsass");
        if (dotsassParam == null || !dotsassParam.equalsIgnoreCase("true")) {
            // If not, continue with the filter chain
            chain.doFilter(request, response);
            return;
        }
        
        try {
            Logger.debug(this, "---- SASS Query Parameter Filter ----");
            
            final Host currentSite = WebAPILocator.getHostWebAPI().getCurrentHost(httpReq);
            final boolean live = !WebAPILocator.getUserWebAPI().isLoggedToBackend(httpReq);
            final User user = WebAPILocator.getUserWebAPI().getLoggedInUser(httpReq);
            final String originalURI = httpReq.getRequestURI();
            
            Logger.debug(this, String.format("-> Original URI = %s", originalURI));
            
            // Handle the two different URL patterns
            if (originalURI.startsWith("/dA/")) {
                // Handle /dA/{identifier} pattern
                handleShortyRequest(httpReq, httpResp, originalURI, currentSite, live, user);
            } else {
                // Handle direct file path pattern
                handleDirectPathRequest(httpReq, httpResp, originalURI, currentSite, live, user);
            }
            
        } catch (Exception e) {
            Logger.error(this, "Error processing SASS compilation request: " + e.getMessage(), e);
            httpResp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error processing SASS compilation request");
        }
    }

    @Override
    public void destroy() {
        // No cleanup needed
    }
    
    /**
     * Handles requests with direct file paths like /path/to/file.scss?dotsass=true
     */
    private void handleDirectPathRequest(HttpServletRequest req, HttpServletResponse resp, 
                                        String originalURI, Host currentSite, boolean live, User user) 
                                        throws ServletException, IOException, DotDataException, DotSecurityException {
        
        // Get the file URI from the request
        String fileUri = originalURI;
        
        // Check if the file exists
        String actualUri = fileUri;
        if (!fileUri.toLowerCase().endsWith(".scss")) {
            actualUri = fileUri + ".scss";
        }
        
        Logger.debug(this, String.format("-> Actual URI = %s", actualUri));
        
        // Find the identifier for the file
        final Identifier ident = APILocator.getIdentifierAPI().find(currentSite, actualUri);
        if (null == ident || !InodeUtils.isSet(ident.getId())) {
            Logger.error(this, String.format("Requested SASS file '%s' in Site '%s' does not exist", actualUri, currentSite));
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        
        // Forward to the SASS preprocessor servlet
        forwardToSassPreprocessor(req, resp, fileUri);
    }
    
    /**
     * Handles requests with shorty IDs like /dA/{identifier}?dotsass=true
     */
    private void handleShortyRequest(HttpServletRequest req, HttpServletResponse resp, 
                                    String originalURI, Host currentSite, boolean live, User user) 
                                    throws ServletException, IOException, DotDataException, DotSecurityException {
        
        // Extract the shorty ID from the URI
        String[] uriParts = originalURI.split("/");
        if (uriParts.length < 3) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid shorty ID format");
            return;
        }
        
        String inodeOrIdentifier = uriParts[2];
        final Optional<ShortyId> shortOpt = this.shortyIdAPI.getShorty(inodeOrIdentifier);
        
        if (shortOpt.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Shorty ID not found");
            return;
        }
        
        final ShortyId shorty = shortOpt.get();
        
        // Get the file path from the shorty ID
        String filePath = null;
        try {
            if (shorty.type == ShortType.IDENTIFIER) {
                // Get the file asset from the identifier
                final long defLang = APILocator.getLanguageAPI().getDefaultLanguage().getId();
                final FileAsset fileAsset = APILocator.getFileAssetAPI().fromContentlet(
                    APILocator.getContentletAPI().findContentletByIdentifier(shorty.longId, live, defLang, user, true)
                );
                
                // Check if it's a SCSS file
                if (!fileAsset.getFileName().toLowerCase().endsWith(".scss")) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Not a SCSS file");
                    return;
                }
                
                // Get the file path
                filePath = fileAsset.getPath() + fileAsset.getFileName();
            } else {
                // Handle other shorty types if needed
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unsupported shorty type");
                return;
            }
        } catch (DotStateException | DotDataException | DotSecurityException e) {
            Logger.error(this, "Error retrieving file from shorty ID: " + e.getMessage(), e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error retrieving file");
            return;
        }
        
        // Forward to the SASS preprocessor servlet
        forwardToSassPreprocessor(req, resp, filePath);
    }
    
    /**
     * Forwards the request to the SASS preprocessor servlet
     */
    private void forwardToSassPreprocessor(HttpServletRequest req, HttpServletResponse resp, String filePath) 
                                          throws ServletException, IOException {
        // Create the path for the SASS preprocessor
        String sassPath = filePath + ".dotsass";
        
        Logger.debug(this, String.format("-> Forwarding to = %s", sassPath));
        
        // Forward the request
        req.getRequestDispatcher(sassPath).forward(req, resp);
    }
}
