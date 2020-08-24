package com.dotcms.graphql.datafetcher.page;

import com.dotcms.graphql.DotGraphQLContext;
import com.dotcms.rendering.velocity.servlet.VelocityModeHandler;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionLevel;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.htmlpageasset.business.render.ContainerRaw;
import com.dotmarketing.portlets.htmlpageasset.business.render.ContainerRenderedBuilder;
import com.dotmarketing.portlets.htmlpageasset.business.render.page.JsonMapper;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.VelocityUtil;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.liferay.portal.model.User;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.io.CharArrayReader;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.velocity.context.Context;

/**
 * This DataFetcher returns a {@link String} containing the rendered HTML code of the requested page
 * requested {@link HTMLPageAsset}.
 */
public class PageRenderDataFetcher implements DataFetcher<String> {
    @Override
    public String get(final DataFetchingEnvironment environment) throws Exception {
        try {
            final User user = ((DotGraphQLContext) environment.getContext()).getUser();
            final HttpServletRequest request = ((DotGraphQLContext) environment.getContext())
                    .getHttpServletRequest();
            final HttpServletResponse response = ((DotGraphQLContext) environment.getContext())
                    .getHttpServletResponse();

            final Contentlet contentlet = environment.getSource();
            final String pageModeAsString = environment.getArgument("pageMode")
                    != null ? environment.getArgument("pageMode") : PageMode.LIVE.name();

            final PageMode mode = PageMode.get(pageModeAsString);

            final HTMLPageAsset pageAsset = APILocator.getHTMLPageAssetAPI()
                    .fromContentlet(contentlet);

            final Host host = WebAPILocator.getHostWebAPI().getCurrentHost(request, user);

            return VelocityModeHandler.modeHandler(pageAsset, mode, request, response, host).eval();

        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
            throw e;
        }
    }
}
