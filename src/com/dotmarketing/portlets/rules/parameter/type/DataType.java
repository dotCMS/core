package com.dotmarketing.portlets.rules.parameter.type;

import com.dotcms.repackage.org.apache.commons.lang.NotImplementedException;
import java.io.Serializable;

/**
 * @author Geoff M. Granum
 */
public abstract class DataType {

    private final String id;

    public DataType(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void checkValid(String value){
        throw new NotImplementedException();
    }
}
 
