package com.dotcms.ai.viewtool;

import com.dotcms.ai.api.EmbeddingsAPI;
import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.app.ConfigService;
import com.dotcms.ai.db.EmbeddingsDTO;
import com.dotcms.ai.util.ContentToStringUtil;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.rendering.velocity.viewtools.content.ContentMap;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SearchTool implements ViewTool {
    final private HttpServletRequest request;
    final private Host host;
    //final private AppConfig app;

    /**
     * $ai.search
     *
     * @param initData
     */
    SearchTool(final Object initData) {
        this.request = ((ViewContext) initData).getRequest();
        this.host = WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(this.request);
        //this.app = ConfigService.INSTANCE.config(this.host);
    }

    @Override
    public void init(final Object initData) {}

    public Object query(final String query, final String indexName) {
        final User user = PortalUtil.getUser(request);
        final EmbeddingsDTO searcher = new EmbeddingsDTO.Builder()
                .withQuery(query)
                .withIndexName(indexName)
                .withUser(user)
                .withLimit(50)
                .withThreshold(.25f)
                .build();

        try {
            return EmbeddingsAPI.impl(host).searchForContent(searcher);
        } catch (Exception e) {
            return Map.of("error", e.getMessage(), "stackTrace", Arrays.asList(e.getStackTrace()));
        }
    }

    public Object query(final Map<String, Object> mapIn) {
        final User user = PortalUtil.getUser(request);
        final EmbeddingsDTO searcher = EmbeddingsDTO.from(mapIn).withUser(user).build();

        try {
            return EmbeddingsAPI.impl(host).searchForContent(searcher);
        } catch (Exception e) {
            return Map.of("error", e.getMessage(), "stackTrace", Arrays.asList(e.getStackTrace()));
        }
    }

    public Object query(final String query) {
        return query(query, "default");
    }



    public Object related(final ContentMap contentMap, final String indexName) {
        return related(contentMap.getContentObject(), indexName);
    }

    public Object related(final Contentlet contentlet, final String indexName) {
        try {
            final User user = PortalUtil.getUser(request);
            final List<Field> fields = ContentToStringUtil.impl.get().guessWhatFieldsToIndex(contentlet);

            final Optional<String> contentToRelate = ContentToStringUtil.impl.get().parseFields(contentlet, fields);
            if (contentToRelate.isEmpty()) {
                return new JSONObject();
            }

            final EmbeddingsDTO searcher = new EmbeddingsDTO.Builder()
                    .withQuery(contentToRelate.get())
                    .withIndexName(indexName)
                    .withExcludeIndentifiers(new String[]{contentlet.getIdentifier()})
                    .withUser(user)
                    .withLimit(50)
                    .withThreshold(.25f)
                    .build();
            return EmbeddingsAPI.impl(host).searchForContent(searcher);
        } catch (Exception e) {
            return Map.of("error", e.getMessage(), "stackTrace", Arrays.asList(e.getStackTrace()));
        }
    }

}
