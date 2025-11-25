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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Optional;
import java.util.UUID;

public class BaseCharacter extends AbstractCharacter {

    private final static String CLUSTER_ID;
    private final static String SERVER_ID;
    private final ShortyIdAPI shorty = APILocator.getShortyAPI();
    static {
        CLUSTER_ID = ClusterFactory.getClusterId();
        SERVER_ID = APILocator.getServerAPI().readServerId();

    }


    private BaseCharacter(final HttpServletRequest request, final HttpServletResponse response, final Visitor visitor) {
        super(request, response, visitor);
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

        final Long pageProcessingTime = (Long) request.getAttribute(VisitorFilter.DOTPAGE_PROCESSING_TIME);
        myMap.get().put("id", UUID.randomUUID().toString());
        myMap.get().put("status", response.getStatus());
        myMap.get().put("iAm", iAm);
        myMap.get().put("uri", uri);
        myMap.get().put("ms", pageProcessingTime);

        myMap.get().put("cluster", shorty.shortify(CLUSTER_ID));
        myMap.get().put("server", shorty.shortify(SERVER_ID));
        myMap.get().put("session", request.getSession().getId());
        myMap.get().put("sessionNew", request.getSession().isNew());
        myMap.get().put("time", System.currentTimeMillis());

        myMap.get().put("mime", response.getContentType());
        myMap.get().put("vanityUrl", (String) request.getAttribute(VisitorFilter.VANITY_URL_ATTRIBUTE));
        myMap.get().put("referer", request.getHeader("referer"));
        myMap.get().put("user-agent", request.getHeader("user-agent"));
        myMap.get().put("host", request.getHeader("host"));
        myMap.get().put("assetId", assetId);
        myMap.get().put("contentId", content.orElse(null));

        myMap.get().put("lang", lang.toString());
        myMap.get().put("langId", lang.getId());
        myMap.get().put("src", "dotCMS");
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

    /**
     * This method will resolve the resource type of the request
     * @param uri
     * @param site
     * @param languageId
     * @return IAm
     */
    public static IAm resolveResourceType(final String uri, final Host site, final long languageId) {


        if(uri!=null) {
            if(isFilePreffixOrSuffix(uri)) {
                return IAm.FILE;
            }
        }

        return CMSUrlUtil.getInstance().resolveResourceType(IAm.NOTHING_IN_THE_CMS, uri,
                site, languageId)._1;

    }

    private static boolean isFilePreffixOrSuffix(String uri) {
        return uri.startsWith("/dotAsset/") ||
                uri.startsWith("/contentAsset") ||
                uri.startsWith("/dA") ||
                uri.startsWith("/DOTLESS") ||
                uri.startsWith("/DOTSASS") ||
                uri.endsWith(".dotsass");
    }


}
