package com.dotcms.graphql.datafetcher.page;

import com.dotcms.graphql.DotGraphQLContext;
import com.dotcms.graphql.exception.PermissionDeniedGraphQLException;
import com.dotcms.rest.api.v1.page.PageResource;
import com.dotcms.vanityurl.business.VanityUrlAPI;
import com.dotcms.vanityurl.model.CachedVanityUrl;
import com.dotcms.vanityurl.model.VanityUrlResult;
import com.dotcms.variant.VariantAPI;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.transform.DotContentletTransformer;
import com.dotmarketing.portlets.contentlet.transform.DotTransformerBuilder;
import com.dotmarketing.portlets.htmlpageasset.business.render.HTMLPageAssetNotFoundException;
import com.dotmarketing.portlets.htmlpageasset.business.render.HTMLPageAssetRenderedAPIImpl.HTMLPageUrl;
import com.dotmarketing.portlets.htmlpageasset.business.render.PageContext;
import com.dotmarketing.portlets.htmlpageasset.business.render.PageContextBuilder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.rules.business.RulesEngine;
import com.dotmarketing.portlets.rules.model.Rule.FireOn;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import io.vavr.control.Try;
import java.time.Instant;
import java.util.Date;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Optional;
/**
 * This DataFetcher returns a {@link HTMLPageAsset} given an URL. It also takes optional parameters
 * to find a specific version of the page: languageId and pageMode.
 *
 * The returned page includes extra properties set by a page transformer.
 *
 */
public class PageDataFetcher implements DataFetcher<Contentlet> {
    @Override
    public Contentlet get(final DataFetchingEnvironment environment) throws Exception {
        try {
            final DotGraphQLContext context = environment.getContext();
            final User user = context.getUser();
            final HttpServletRequest request = ((DotGraphQLContext) environment.getContext())
                    .getHttpServletRequest();

            final HttpServletResponse response = ((DotGraphQLContext) environment.getContext())
                    .getHttpServletResponse();

            final String url = environment.getArgument("url");
            final String languageId = UtilMethods.isSet((String) environment.getArgument("languageId"))
                    ? environment.getArgument("languageId") :
                    String.valueOf(APILocator.getLanguageAPI().getDefaultLanguage().getId());
            final String pageModeAsString = environment.getArgument("pageMode")
                    != null ? environment.getArgument("pageMode") : PageMode.LIVE.name();
            final boolean fireRules = environment.getArgument("fireRules");
            final String persona = environment.getArgument("persona");
            final String site = environment.getArgument("site");
            final String publishDate = environment.getArgument("publishDate");
            final String variantName = environment.getArgument("variantName");

            context.addParam("url", url);
            context.addParam("languageId", languageId);
            context.addParam("pageMode", pageModeAsString);
            context.addParam("fireRules", fireRules);
            context.addParam("persona", persona);
            context.addParam("site", site);
            context.addParam("publishDate", publishDate);

            final PageMode mode = PageMode.get(pageModeAsString);
            PageMode.setPageMode(request, mode);

            // we need to set the language to the request
            if(UtilMethods.isSet(languageId)) {
                request.setAttribute(WebKeys.HTMLPAGE_LANGUAGE, languageId);
            }

            if(UtilMethods.isSet(persona)) {
                request.setAttribute(WebKeys.CMS_PERSONA_PARAMETER, persona);
            }

            if(UtilMethods.isSet(site)) {
                request.setAttribute(Host.HOST_VELOCITY_VAR_NAME, site);
            }

            if(UtilMethods.isSet(variantName)) {
                request.setAttribute(VariantAPI.VARIANT_KEY, variantName);
            }

            Date publishDateObj = null;

            if(UtilMethods.isSet(publishDate)) {
                publishDateObj = Try.of(()-> DateUtil.convertDate(publishDate)).getOrElse(() -> {
                    Logger.error(this, "Invalid publish date: " + publishDate);
                    return null;
                });
                if(null != publishDateObj) {
                    //We get a valid time machine date
                    final Instant instant = publishDateObj.toInstant();
                    final long epochMilli = instant.toEpochMilli();
                    context.addParam(PageResource.TM_DATE, epochMilli);
                    request.setAttribute(PageResource.TM_DATE, epochMilli);
                }
            }

            // Vanity URL resolution
            String resolvedUri = url;
            final Language language = UtilMethods.isSet(languageId) ?
                    APILocator.getLanguageAPI().getLanguage(languageId) : APILocator.getLanguageAPI().getDefaultLanguage();
            final Host host = WebAPILocator.getHostWebAPI().getHost(request);

            final Optional<CachedVanityUrl> cachedVanityUrlOpt = APILocator.getVanityUrlAPI()
                    .resolveVanityUrl(url, host, language);

            if (cachedVanityUrlOpt.isPresent()) {
                response.setHeader(VanityUrlAPI.VANITY_URL_RESPONSE_HEADER,
                        cachedVanityUrlOpt.get().vanityUrlId);

                // Store the CachedVanityUrl in the context
                context.addParam("cachedVanityUrl", cachedVanityUrlOpt.get());

                if (cachedVanityUrlOpt.get().isTemporaryRedirect() || 
                    cachedVanityUrlOpt.get().isPermanentRedirect()) {
                    // For redirects, return an empty page with vanity URL info
                    final Contentlet emptyPage = new Contentlet();
                    emptyPage.setLanguageId(language.getId());
                    emptyPage.setHost(host.getIdentifier());
                    context.markAsVanityRedirect();
                    return emptyPage;
                } else {
                    // For forwards, use the resolved URI
                    final VanityUrlResult vanityUrlResult = cachedVanityUrlOpt.get()
                            .handle(url);
                    resolvedUri = vanityUrlResult.getRewrite();
                }
            }

            Logger.debug(this, ()-> "Fetching page for URL: " + url);

            final PageContext pageContext = PageContextBuilder.builder()
                    .setUser(user)
                    .setPageUri(resolvedUri)
                    .setPageMode(mode)
                    .setGraphQL(true)
                    .build();

            HTMLPageUrl pageUrl;

            try {
                pageUrl = APILocator.getHTMLPageAssetRenderedAPI()
                        .getHtmlPageAsset(pageContext, request);
            } catch (HTMLPageAssetNotFoundException e) {
                Logger.error(this, e.getMessage());
                return null;
            } catch (DotSecurityException e) {
                if(mode.equals(PageMode.WORKING)) {
                    throw new PermissionDeniedGraphQLException(
                            "Unauthorized: You do not have the necessary permissions to request this page in edit mode.");
                }
                throw new PermissionDeniedGraphQLException();
            }

            final HTMLPageAsset pageAsset = pageUrl.getHTMLPage();
            context.addParam("page", pageAsset);
            pageAsset.getMap().put("URLMapContent", pageUrl.getUrlMapInfo());


            if(fireRules) {
                Logger.info(this, "Rules will be fired");
                request.setAttribute("fireRules", true);
                RulesEngine.fireRules(request, response, pageAsset, FireOn.EVERY_PAGE);
            }

            final DotContentletTransformer transformer = new DotTransformerBuilder()
                    .graphQLDataFetchOptions().content(pageAsset).forUser(user).build();

            final Contentlet out = transformer.hydrate().get(0);
            // PageResource add this property to the map at Serializer
            // Level that's why it is not part of the Transformers logic
            final String urlMapper = pageUrl.getPageUrlMapper();
            if(StringUtils.isSet(urlMapper)) {
               out.getMap().put("pageURI", urlMapper);
            }
            return out;

        } catch (Exception e) {
            Logger.error(this, e.getMessage());
            throw e;
        }
    }
}
