package com.dotcms.graphql.datafetcher.page;

import com.dotcms.graphql.DotGraphQLContext;
import com.dotcms.rendering.velocity.services.PageRenderUtil;
import com.dotmarketing.portlets.htmlpageasset.business.render.ContainerRaw;
import com.dotmarketing.portlets.htmlpageasset.business.render.ContainerRenderedBuilder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.VelocityUtil;
import com.liferay.portal.model.User;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.velocity.context.Context;

/**
 * This DataFetcher returns the {@link TemplateLayout} associated to the requested {@link HTMLPageAsset}.
 */
public class RenderedContainersDataFetcher implements DataFetcher<Set<Entry<String, String>>> {
    @Override
    public Set<Entry<String, String>> get(final DataFetchingEnvironment environment) throws Exception {
        try {
            final DotGraphQLContext context = environment.getContext();
            final User user = context.getUser();
            final ContainerRaw containerRaw = environment.getSource();
            final String pageModeAsString = (String) context.getParam("pageMode");
            final String languageId = (String) context.getParam("languageId");

            final PageMode mode = PageMode.get(pageModeAsString);
            final HttpServletRequest request = context.getHttpServletRequest();
            final HttpServletResponse response = context.getHttpServletResponse();

            final PageRenderUtil pageRenderUtil = (PageRenderUtil) context.getParam("pageRenderUtil");

            final Context velocityContext  = pageRenderUtil
                    .addAll(VelocityUtil.getInstance().getContext(request, response));

            final Map<String, String> uuidsRendered = ContainerRenderedBuilder.
                    render(velocityContext, mode, containerRaw);

            return uuidsRendered.entrySet();
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
            throw e;
        }
    }
}
