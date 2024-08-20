package com.dotcms.visitor.filter.characteristics;

import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotcms.util.HttpRequestDataUtil;
import com.dotcms.uuid.shorty.ShortyIdAPI;
import com.dotcms.vanityurl.model.CachedVanityUrl;
import com.dotcms.visitor.domain.Visitor;
import com.dotcms.visitor.filter.servlet.VisitorFilter;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.filters.CMSFilter.IAm;
import com.dotmarketing.filters.CMSUrlUtil;
import com.dotmarketing.filters.Constants;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.WebKeys;
import io.vavr.control.Try;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class BaseCharacter implements Character {

    private final static String CLUSTER_ID;
    private final static String SERVER_ID;
    private final ShortyIdAPI shorty = APILocator.getShortyAPI();
    static {
        CLUSTER_ID = ClusterFactory.getClusterId();
        SERVER_ID = APILocator.getServerAPI().readServerId();
    }


    public Map<String, Serializable> getMap(final HttpServletRequest request, final HttpServletResponse response, final Visitor visitor) {

        clearMap();
        final Object asset = (request.getAttribute("idInode") != null) ? request.getAttribute("idInode")
                : (Identifier) request.getAttribute(Constants.CMS_FILTER_IDENTITY);

        final String assetId = (asset instanceof Identifier) ? ((Identifier) asset).getId()
                : (asset != null && asset instanceof String) ? (String) asset : null;

        String uri = null;
        try {
            uri = URLDecoder.decode((request.getAttribute(Constants.CMS_FILTER_URI_OVERRIDE) != null)
                    ? (String) request.getAttribute(Constants.CMS_FILTER_URI_OVERRIDE)
                    : request.getRequestURI(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            // e.printStackTrace();
        }


        final Optional<String> content = Optional.ofNullable((String) request.getAttribute(WebKeys.WIKI_CONTENTLET));
        final Language lang = WebAPILocator.getLanguageWebAPI().getLanguage(request);
        final IAm iAm = resolveResourceType(uri, getHostNoThrow(request), lang.getId());
        final long pageProcessingTime = (Long) request.getAttribute(VisitorFilter.DOTPAGE_PROCESSING_TIME);
        
        accrue("id", UUID.randomUUID().toString());
        accrue("status", response.getStatus());
        accrue("iAm", iAm);
        accrue("uri", uri);
        accrue("ms", pageProcessingTime);
        accrue("method", request.getMethod());
        accrue("cluster", shorty.shortify(CLUSTER_ID));
        accrue("server", shorty.shortify(SERVER_ID));
        accrue("session", (request.getSession(false)!=null) ? request.getSession().getId() : "n/a");
        accrue("sessionNew", (request.getSession(false)!=null) ? request.getSession().isNew() : false);
        accrue("time", System.currentTimeMillis());

        accrue("mime", response.getContentType());
        
        CachedVanityUrl cachedVanity = (CachedVanityUrl) request.getAttribute(Constants.VANITY_URL_OBJECT);
        if(cachedVanity!=null) {
            accrue("vanityUrl", cachedVanity.vanityUrlId);
        }   
        accrue("referer", request.getHeader("referer"));
        accrue("host", request.getHeader("host"));
        accrue("assetId", assetId);
        accrue("contentId", content.orElse(null));

        accrue("lang", lang.toString());
    }

    public BaseCharacter(final HttpServletRequest request, final HttpServletResponse response) {
        this(request, response, resolveVisitor(request));
    }


    private static Visitor resolveVisitor(HttpServletRequest request) {
        
        if( APILocator.getVisitorAPI().getVisitor(request, false).isPresent()) {
            return APILocator.getVisitorAPI().getVisitor(request).get();
        }
        else {
            Visitor visitor = Visitor.newInstance(request);
            visitor.setIpAddress( Try.of(()-> HttpRequestDataUtil.getIpAddress(request)).getOrNull());
            return visitor;
        }
                      
       
    }

    private Host getHostNoThrow(HttpServletRequest req) {
        try {
            return WebAPILocator.getHostWebAPI().getCurrentHost(request);
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }

    }



    private IAm resolveResourceType(final String uri, final Host site, final long languageId) {


        if(uri!=null) {
            if(uri.startsWith("/dotAsset/") || uri.startsWith("/contentAsset") || uri.startsWith("/dA") || uri.startsWith("/DOTLESS")|| uri.startsWith("/DOTSASS")) {
                return IAm.FILE;
            }
        }
        
        
        
        
        
        if (CMSUrlUtil.getInstance().isFileAsset(uri, site, languageId)) {
            return IAm.FILE;
        } else if (CMSUrlUtil.getInstance().isPageAsset(uri, site, languageId)) {
            return IAm.PAGE;
        } else if (CMSUrlUtil.getInstance().isFolder(uri, site)) {
            return IAm.FOLDER;
        } else {
            return IAm.NOTHING_IN_THE_CMS;
        }

    }


}
