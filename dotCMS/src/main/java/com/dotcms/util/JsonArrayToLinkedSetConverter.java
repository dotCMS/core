package com.dotcms.util;

import com.dotcms.repackage.org.codehaus.jettison.json.JSONArray;
import com.dotcms.repackage.org.codehaus.jettison.json.JSONException;

import java.util.LinkedHashSet;

/**
 * Converts a Json Array to the linked set.
 * @author  jsanca
 */
public class JsonArrayToLinkedSetConverter<T> implements Converter<JSONArray, LinkedHashSet<T>> {

    public static final LinkedHashSet EMPTY_LINKED_SET = new LinkedHashSet();
    private final Converter<Object, T> itemConverter;

    public JsonArrayToLinkedSetConverter(final Converter<Object, T> itemConverter) {
        this.itemConverter = itemConverter;
    }

    @Override
    public LinkedHashSet<T> convert(final JSONArray jsonArray) {

        final LinkedHashSet<T> linkedSet = new LinkedHashSet<>();

        try {
            for (int i = 0; i < jsonArray.length(); ++i) {

                linkedSet.add(this.itemConverter.convert(jsonArray.get(i)));
            }
        } catch (JSONException e) {

            return EMPTY_LINKED_SET;
        }

        return linkedSet;
    }
}
