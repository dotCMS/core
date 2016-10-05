package com.dotcms.rest.api.v1.content;


import com.dotmarketing.portlets.structure.model.Structure;

import java.util.*;


/**
 * Collection of {@link ContentTypeView}
 */
public class BaseContentTypesViewCollection {

    private final Map<String, List<ContentTypeView>> contentTypeViews = new LinkedHashMap<>();

    public void add (Structure structure, ContentTypeView contentTypeView){
        String structureName = Structure.Type.getType(structure.getStructureType()).name();
        List<ContentTypeView> contentTypeViews = this.contentTypeViews.get(structureName);

        if (contentTypeViews == null){
            contentTypeViews = new ArrayList<>();
            this.contentTypeViews.put(structureName, contentTypeViews);
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
