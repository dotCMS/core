package com.dotcms.graphql.datafetcher;

import static com.dotcms.rest.api.v1.page.NavResource.navToMap;

import com.dotcms.graphql.DotGraphQLContext;
import com.dotcms.graphql.exception.InvalidLanguageException;
import com.dotcms.graphql.exception.ResourceNotFoundException;
import com.dotcms.rendering.velocity.viewtools.navigation.NavResult;
import com.dotcms.rendering.velocity.viewtools.navigation.NavTool;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.VelocityUtil;
import com.liferay.util.StringPool;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.velocity.tools.view.context.ChainedContext;
import org.apache.velocity.tools.view.context.ViewContext;

/**
 * DataFetcher for the Navigation GraphQL type.
 */
public class NavigationDataFetcher implements DataFetcher<Map<String, Object>>  {

    /**
     * This method is called by the GraphQL engine to fetch the data for the field.
     * @param environment this is the data fetching environment which contains all the context you need to fetch a value
     *
     * @return a map containing the navigation data
     * @throws Exception if an error occurs while fetching the data
     */
    @Override
    public Map<String, Object> get(DataFetchingEnvironment environment) throws Exception {
        final long defLangId = APILocator.getLanguageAPI().getDefaultLanguage().getId();
        final int maxDepth = 1;
        final DotGraphQLContext context = environment.getContext();
        final HttpServletRequest request = context.getHttpServletRequest();
        final HttpServletResponse response = context.getHttpServletResponse();
        final long languageId = getArgument(environment, "languageId", defLangId);
        final String uri = getArgument(environment, "uri", StringPool.FORWARD_SLASH);
        final Long depth = getArgument(environment, "depth", (long) maxDepth);

        if (APILocator.getLanguageAPI().getLanguages().stream()
                .noneMatch(l -> l.getId() == languageId)) {
            throw new InvalidLanguageException(languageId);
        }

        final ViewContext ctx = new ChainedContext(
                VelocityUtil.getBasicContext(),
                request,
                response, Config.CONTEXT
        );

        final String path = (!uri.startsWith(StringPool.FORWARD_SLASH)) ? StringPool.FORWARD_SLASH + uri : uri;
        //Force NavTool to behave as Live when rendering items
        PageMode.setPageMode(request, PageMode.LIVE, false);
        final NavTool tool = new NavTool();
        tool.init(ctx);
        final NavResult nav = tool.getNav(path, languageId);
        if (null == nav) {
            throw new ResourceNotFoundException();
        }

        return navToMap(nav,  depth.intValue(), 1);

    }

    /**
     * Get a string argument from the environment, or return a default value if not present.
     * @param environment this is the data fetching environment which contains all the context you need to fetch a value
     * @param name the name of the argument
     * @param defaultValue the default value to return if the argument is not present
     * @return the value of the argument or the default value
     */
    private String getArgument(DataFetchingEnvironment environment, String name, String defaultValue) {
        final Object value = environment.getArgument(name);
        if (value == null) {
            return defaultValue;
        }
        return String.valueOf(value);
    }

    /**
     * Get an integer argument from the environment, or return a default value if not present.
     * @param environment this is the data fetching environment which contains all the context you need to fetch a value
     * @param name the name of the argument
     * @param defaultValue the default value to return if the argument is not present
     * @return the value of the argument or the default value
     */
    private Long getArgument(DataFetchingEnvironment environment, String name, Long defaultValue) {
        final Object value = environment.getArgument(name);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
