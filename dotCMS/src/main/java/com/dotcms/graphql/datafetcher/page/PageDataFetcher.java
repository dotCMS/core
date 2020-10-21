package com.dotcms.graphql.datafetcher.page;

import com.dotcms.graphql.DotGraphQLContext;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.transform.DotContentletTransformer;
import com.dotmarketing.portlets.contentlet.transform.DotTransformerBuilder;
import com.dotmarketing.portlets.htmlpageasset.business.render.HTMLPageAssetNotFoundException;
import com.dotmarketing.portlets.htmlpageasset.business.render.HTMLPageAssetRenderedAPIImpl.HTMLPageUrl;
import com.dotmarketing.portlets.htmlpageasset.business.render.PageContext;
import com.dotmarketing.portlets.htmlpageasset.business.render.PageContextBuilder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.rules.business.RulesEngine;
import com.dotmarketing.portlets.rules.model.Rule.FireOn;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
            final String languageId = environment.getArgument("languageId");
            final String pageModeAsString = environment.getArgument("pageMode")
                    != null ? environment.getArgument("pageMode") : PageMode.LIVE.name();
            final boolean fireRules = environment.getArgument("fireRules");

            context.addParam("url", url);
            context.addParam("languageId", languageId);
            context.addParam("pageMode", pageModeAsString);
            context.addParam("fireRules", fireRules);

            final PageMode mode = PageMode.get(pageModeAsString);
            PageMode.setPageMode(request, mode);

            // we need to set the language to the request
            if(UtilMethods.isSet(languageId)) {
                request.setAttribute(WebKeys.HTMLPAGE_LANGUAGE, languageId);
            }

            final PageContext pageContext = PageContextBuilder.builder()
                    .setUser(user)
                    .setPageUri(url)
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

            return transformer.hydrate().get(0);

        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
            throw e;
        }
    }
}
