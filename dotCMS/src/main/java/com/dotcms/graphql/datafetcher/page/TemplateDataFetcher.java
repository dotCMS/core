package com.dotcms.graphql.datafetcher.page;

import com.dotcms.graphql.DotGraphQLContext;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.transform.DotContentletTransformer;
import com.dotmarketing.portlets.contentlet.transform.DotTransformerBuilder;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.htmlpageasset.business.render.page.JsonMapper;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.liferay.portal.model.User;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.io.CharArrayReader;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import javax.servlet.http.HttpServletRequest;

/**
 * This DataFetcher returns a {@link HTMLPageAsset} given an URL. It also takes optional parameters
 * to find a specific version of the page: languageId and pageMode.
 *
 * The returned page includes extra properties set by a page transformer.
 *
 */
public class TemplateDataFetcher implements DataFetcher<Map<Object, Object>> {
    @Override
    public Map<Object, Object> get(final DataFetchingEnvironment environment) throws Exception {
        try {
            final User user = ((DotGraphQLContext) environment.getContext()).getUser();
            final HttpServletRequest request = ((DotGraphQLContext) environment.getContext())
                    .getHttpServletRequest();

            final Contentlet contentlet = environment.getSource();

            final String pageModeAsString = environment.getArgument("pageMode")
                    != null ? environment.getArgument("pageMode") : PageMode.LIVE.name();

            final PageMode mode = PageMode.get(pageModeAsString);

            final String templateId = contentlet.getStringProperty(HTMLPageAssetAPI.TEMPLATE_FIELD);

            final Template template = getTemplate(templateId, mode);

            return asMap(template);
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
            throw e;
        }
    }

    private Template getTemplate(final String templateId,
            final PageMode mode) throws DotDataException {
        try {
            final User systemUser = APILocator.getUserAPI().getSystemUser();

            return mode.showLive ?
                    (com.dotmarketing.portlets.templates.model.Template)
                            APILocator.getVersionableAPI()
                                    .findLiveVersion(templateId, systemUser, mode.respectAnonPerms)
                    :
                    (com.dotmarketing.portlets.templates.model.Template)
                            APILocator.getVersionableAPI()
                                    .findWorkingVersion(templateId, systemUser, mode.respectAnonPerms);
        } catch (DotSecurityException e) {
            return null;
        }
    }

    private Map<Object, Object> asMap(final Object object)  {
        final ObjectWriter objectWriter = JsonMapper.mapper.writer().withDefaultPrettyPrinter();

        try {
            final String json = objectWriter.writeValueAsString(object);
            final Map map = JsonMapper.mapper.readValue(new CharArrayReader(json.toCharArray()), Map.class);

            map.values().removeIf(Objects::isNull);
            return map;
        } catch (IOException e) {
            throw new DotRuntimeException(e);
        }
    }
}
