package com.dotmarketing.portlets.rules.parameter.display;

import com.dotmarketing.portlets.rules.parameter.type.DataType;

/**
 * @author Geoff M. Granum
 */
public class Input<T extends DataType> {

    private final String id;
    private final T dataType;

    public Input(String id, T dataType) {
        this.id = id;
        this.dataType = dataType;
    }

    public String getId() {
        return id;
    }

    public T getDataType() {
        return dataType;
    }
}
 
