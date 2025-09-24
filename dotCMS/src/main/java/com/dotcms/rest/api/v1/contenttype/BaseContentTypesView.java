package com.dotcms.rest.api.v1.contenttype;

import java.util.List;

/**
 * It represent a Structure.Type in the View, it contents a List of the {@link ContentTypeView} for each Structure.Type
 */
public class BaseContentTypesView {

    private final String name;
    private final String label;
    private final List<ContentTypeView> types;
    private int index;

    BaseContentTypesView(String name, String label, List<ContentTypeView> types){
        this.name = name;
        this.types = types;
        this.label = label;
    }

    BaseContentTypesView(String name, String label, List<ContentTypeView> types, int index) {
        this(name, label, types);
        this.index = index;
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

    public int getIndex() {
        return index;
    }
}
