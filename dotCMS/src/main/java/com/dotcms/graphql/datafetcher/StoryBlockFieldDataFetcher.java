package com.dotcms.graphql.datafetcher;

import com.dotcms.repackage.org.codehaus.jettison.json.JSONObject;
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
        final JSONObject jsonContFieldValue = new JSONObject(contFieldValue);
        storyBlockMap.put("render",jsonContFieldValue.get("render").toString());
        //Remove Render property from the json, since it's another sub-fieldType
        jsonContFieldValue.remove("render");
        storyBlockMap.put("json",new ObjectMapper().readValue(jsonContFieldValue.toString(), HashMap.class));

        return storyBlockMap;
    }
}
