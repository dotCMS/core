package com.dotcms.graphql.datafetcher;

import com.dotmarketing.util.json.JSONObject;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.HashMap;
import java.util.Map;

public class StoryBlockFieldDataFetcher implements DataFetcher<Map<String, Object>> {
    @Override
    public Map<String, Object> get(DataFetchingEnvironment environment) throws Exception {
        final Contentlet contentlet = environment.getSource();
        final String variableName = environment.getField().getName();
        final Map<String, Object> storyBlockMap = new HashMap<>();

        final String contFieldValue = contentlet.getStringProperty(variableName);
        final JSONObject jsonContFieldValue = contFieldValue!=null
                ? new JSONObject(contFieldValue)
                : new JSONObject();
        storyBlockMap.put("json",new ObjectMapper().readValue(jsonContFieldValue.toString(), HashMap.class));

        return storyBlockMap;
    }
}
