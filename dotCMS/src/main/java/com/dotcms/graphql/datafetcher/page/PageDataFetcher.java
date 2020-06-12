package com.dotcms.graphql.datafetcher.page;

import com.dotcms.graphql.DotGraphQLContext;
import com.dotcms.util.CollectionsUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.transform.DotContentletTransformer;
import com.dotmarketing.portlets.contentlet.transform.DotTransformerBuilder;
import com.dotmarketing.portlets.htmlpageasset.business.render.HTMLPageAssetRenderedAPIImpl.HTMLPageUrl;
import com.dotmarketing.portlets.htmlpageasset.business.render.PageContext;
import com.dotmarketing.portlets.htmlpageasset.business.render.PageContextBuilder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

public class PageDataFetcher implements DataFetcher<Contentlet> {
    @Override
    public Contentlet get(final DataFetchingEnvironment environment) throws Exception {
        try {
            final User user = ((DotGraphQLContext) environment.getContext()).getUser();
            final HttpServletRequest request = ((DotGraphQLContext) environment.getContext())
                    .getHttpServletRequest();

            final String url = environment.getArgument("url");
            final String languageId = environment.getArgument("languageId");
            final String pageModeAsString = environment.getArgument("pageMode")
                    != null ? environment.getArgument("pageMode") : PageMode.LIVE.name();

            PageMode mode = PageMode.get(pageModeAsString);

            // we need to set the language to the request
            if(UtilMethods.isSet(languageId)) {
                request.setAttribute(WebKeys.HTMLPAGE_LANGUAGE, languageId);
            }

            PageContext pageContext = PageContextBuilder.builder()
                    .setUser(user)
                    .setPageUri(url)
                    .setPageMode(mode)
                    .build();

            HTMLPageUrl pageUrl = APILocator.getHTMLPageAssetRenderedAPI()
                    .getHtmlPageAsset(pageContext, request);

            HTMLPageAsset pageAsset = (HTMLPageAsset) pageUrl.getHTMLPage();
            pageAsset.getMap().put("URLMapContent", pageUrl.getUrlMapInfo());

            final DotContentletTransformer transformer = new DotTransformerBuilder()
                    .graphQLDataFetchOptions().content(pageAsset).build();

            return transformer.hydrate().get(0);
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
            throw e;
        }
    }
}
