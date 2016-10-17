package com.dotcms.rest.api.v1.content;

import java.util.List;

/**
 * It represent a Structure.Type in the View, it contents a List of the {@link ContentTypeView} for each Structure.Type
 */
public class BaseContentTypesView {

    private final String name;
    private final String label;
    private final List<ContentTypeView> types;

    BaseContentTypesView(String name, String label, List<ContentTypeView> types){
        this.name = name;
        this.types = types;
        this.label = label;
    }

    public String getName() {
        return name;
    }

    public List<ContentTypeView> getTypes() {
        return types;
    }

    public String getLabel() {
        return label;
    }
}
