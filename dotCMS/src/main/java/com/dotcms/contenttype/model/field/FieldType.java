package com.dotcms.contenttype.model.field;

import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;

import static com.dotcms.util.CollectionsUtils.imap;
import static com.dotcms.util.CollectionsUtils.toImmutableList;

/**
 * Intances of the class FieldType represent a Field's Type.
 * it provide Field's Type meta data.
 */
public class FieldType implements Comparable<FieldType>{
    private String id;
    private String label;
    private Collection<ContentTypeFieldProperties> properties;
    private String helpText;
    private String clazz;

    public FieldType(String id, String label, Collection<ContentTypeFieldProperties> properties, String helpText, String clazz) {
        this.id = id;
        this.label = label;
        this.properties = properties;
        this.helpText = helpText;
        this.clazz = clazz;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public Collection<ContentTypeFieldProperties> getProperties() {
        return properties;
    }

    public String getHelpText() {
        return helpText;
    }

    public Map<String, Object> toMap(){
        final ImmutableList<String> propertiesName = properties.stream()
                .map(ContentTypeFieldProperties::getName)
                .collect(toImmutableList());

        return imap("id", id,
                "label", label,
                "properties", propertiesName,
                "clazz", clazz,
                "helpText", helpText);
    }

    public String getClazz() {
        return clazz;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        FieldType fieldType = (FieldType) o;

        return id != null ? id.equals(fieldType.id) : fieldType.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }


    @Override
    public int compareTo(@NotNull FieldType o) {
        return label.compareTo(o.label);
    }
}
