package com.dotmarketing.portlets.rules.parameter.type;

import com.dotcms.repackage.org.apache.commons.lang.NotImplementedException;
import java.io.Serializable;

/**
 * @author Geoff M. Granum
 */
public abstract class DataType implements Serializable {

    public static final DataType TEXT = new TextType();
    public static final DataType NUMERIC = new NumericType();

    private static final long serialVersionUID = 1L;
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
 
