package com.dotcms.rest.api.v1.content;

import java.util.List;

/**
 * It represent a Structure.Type in the View, it contents a List of the {@link ContentTypeView} for each Structure.Type
 */
public class StructureTypeView {

    private String name;
    private String label;
    private List<ContentTypeView> types;

    StructureTypeView(String name, String label, List<ContentTypeView> types){
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
