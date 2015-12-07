package com.dotmarketing.portlets.rules.parameter.type;

import com.dotcms.repackage.com.google.common.collect.Lists;
import com.dotcms.repackage.org.apache.commons.lang.NotImplementedException;
import com.dotmarketing.portlets.rules.parameter.comparison.Comparison;
import java.util.List;

/**
 * @author Geoff M. Granum
 */
public abstract class DataType<T> {

    private final String id;
    private List<Comparison<?>> restrictions = Lists.newArrayList();

    public DataType(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public abstract T convert(String from);

    public void checkValid(String value){
        throw new NotImplementedException();
    }

    public DataType<T> restrict(Comparison<?> restriction){
        this.restrictions.add(restriction);
        return this;
    }
}
 
