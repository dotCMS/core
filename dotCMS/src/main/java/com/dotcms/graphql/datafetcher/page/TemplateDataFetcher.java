package com.dotcms.graphql.datafetcher.page;

import com.dotcms.graphql.DotGraphQLContext;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionLevel;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
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

/**
 * This DataFetcher returns a {@link Map} representing a {@link Template} associated to the originally
 * requested {@link HTMLPageAsset}.
 */
public class TemplateDataFetcher implements DataFetcher<Map<Object, Object>> {
    @Override
    public Map<Object, Object> get(final DataFetchingEnvironment environment) throws Exception {
        try {
            final DotGraphQLContext context = environment.getContext();
            final User user = context.getUser();
            final Contentlet contentlet = environment.getSource();

            final String pageModeAsString = (String) context.getParam("pageMode");

            final PageMode mode = PageMode.get(pageModeAsString);

            final String templateId = contentlet.getStringProperty(HTMLPageAssetAPI.TEMPLATE_FIELD);

            Logger.debug(this, ()-> "Fetching template: " + templateId);

            final Template template = getTemplate(templateId, mode);

            return asMap(template, user);
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
                    APILocator.getTemplateAPI().findLiveTemplate(templateId,systemUser, mode.respectAnonPerms) :
                    APILocator.getTemplateAPI().findWorkingTemplate(templateId,systemUser, mode.respectAnonPerms);
        } catch (DotSecurityException e) {
            return null;
        }
    }

    private Map<Object, Object> asMap(final Template template, final User user)  {
        final ObjectWriter objectWriter = JsonMapper.mapper.writer().withDefaultPrettyPrinter();

        try {
            final String json = objectWriter.writeValueAsString(template);
            final Map map = JsonMapper.mapper.readValue(new CharArrayReader(json.toCharArray()), Map.class);

            map.values().removeIf(Objects::isNull);

            boolean canEditTemplate = false;
            try {
                canEditTemplate = APILocator.getPermissionAPI().
                        doesUserHavePermission(template, PermissionLevel.EDIT.getType(), user);
            } catch (DotDataException e) {
                Logger.error(this, e.getMessage());
            }

            map.put("canEdit", canEditTemplate);

            return map;
        } catch (IOException e) {
            throw new DotRuntimeException(e);
        }
    }
}
