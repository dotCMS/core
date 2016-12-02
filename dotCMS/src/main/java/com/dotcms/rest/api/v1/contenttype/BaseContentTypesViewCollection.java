package com.dotcms.rest.api.v1.contenttype;


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * Collection of {@link ContentTypeView}
 */
public class BaseContentTypesViewCollection {

    private final Map<String, List<ContentTypeView>> contentTypeViews = new LinkedHashMap<>();


    public void add (ContentTypeView contentTypeView){
        add(contentTypeView, this.contentTypeViews);
    }

    private void add (ContentTypeView contentTypeView,
                      Map<String, List<ContentTypeView>> contentTypeViewsMap){

        String baseContentTypeName = contentTypeView.getType();
        List<ContentTypeView> contentTypeViews = contentTypeViewsMap.get(baseContentTypeName);


        if (contentTypeViews == null){
            contentTypeViews = new ArrayList<>();
            contentTypeViewsMap.put(baseContentTypeName, contentTypeViews);
        }

        contentTypeViews.add(contentTypeView);
    }

    public List<BaseContentTypesView> getStructureTypeView(Map<String, String> strTypeNames){
        List<BaseContentTypesView> result = new ArrayList<>();

        for (Map.Entry<String, List<ContentTypeView>> contentTypeViewsEntry : contentTypeViews.entrySet()) {
            String name = contentTypeViewsEntry.getKey();
            List<ContentTypeView> types = contentTypeViewsEntry.getValue();
            String label = strTypeNames.get(name);
            result.add(new BaseContentTypesView(name, label, types));
        }

        return result;
    }
}
