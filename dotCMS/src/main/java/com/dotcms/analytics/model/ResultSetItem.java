package com.dotcms.analytics.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ResultSetItem implements Serializable {

    private Map<String, Object> item;

    public ResultSetItem(final Map<String, Object> item) {
        this.item = item;
    }

    /**
     * Return the value of the specific attribute
     * @param attributeName
     * @return
     */
    public Optional<Object> get(final String attributeName){
        return Optional.ofNullable(item.get(attributeName));
    }

    /**
     * Return all the attributes and values.
     *
     * @return
     */
    public Map<String, Object> getAll(){
        return new HashMap<>(item);
    }

    @Override
    public String toString() {
        return "ResultSetItem{" +
                "item=" + item +
                '}';
    }
}
