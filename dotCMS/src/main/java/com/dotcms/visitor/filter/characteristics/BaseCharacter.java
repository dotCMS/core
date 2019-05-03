package com.dotcms.visitor.filter.characteristics;

import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotcms.uuid.shorty.ShortyIdAPI;
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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class BaseCharacter extends AbstractCharacter {



    private BaseCharacter(final HttpServletRequest request, final HttpServletResponse response, final Visitor visitor) {
        super(request, response, visitor);
        clearMap();
        final String CLUSTER_ID = ClusterFactory.getClusterId();
        final String SERVER_ID = APILocator.getServerAPI().readServerId();
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
        IAm iAm = resolveResourceType(uri, getHostNoThrow(request), lang.getId());
        final long pageProcessingTime = (Long) request.getAttribute(VisitorFilter.DOTPAGE_PROCESSING_TIME);
        final Host host = WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request);
            

        
        myMap.get().put("id", UUID.randomUUID().toString());
        myMap.get().put("status", response.getStatus());
        myMap.get().put("iAm", iAm);
        myMap.get().put("url", request.getRequestURL() + (request.getQueryString()!=null ? "?" + request.getQueryString() : ""));
        myMap.get().put("ms", pageProcessingTime);
        myMap.get().put("hostId", host.getIdentifier());
        myMap.get().put("cluster", CLUSTER_ID);
        myMap.get().put("server", SERVER_ID);
        myMap.get().put("session", request.getSession().getId());
        myMap.get().put("sessionNew", (request.getSession(false)!=null) && request.getSession(false).isNew());
        myMap.get().put("time", Instant.now().getEpochSecond());
        myMap.get().put("utc_time", Instant.now().atOffset(ZoneOffset.UTC).toInstant().getEpochSecond());
        myMap.get().put("mime", response.getContentType());
        myMap.get().put("vanityUrl", (String) request.getAttribute(VisitorFilter.VANITY_URL_ATTRIBUTE));
        myMap.get().put("referer", request.getHeader("referer"));
        myMap.get().put("host", request.getHeader("host"));
        myMap.get().put("assetId", assetId);
        myMap.get().put("contentId", content.orElse(null));

        myMap.get().put("lang", lang.toString());
    }

    public BaseCharacter(final HttpServletRequest request, final HttpServletResponse response) {
        this(request, response, APILocator.getVisitorAPI().getVisitor(request).get());
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
