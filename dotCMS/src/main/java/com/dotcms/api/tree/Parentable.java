package com.dotcms.api.tree;

/**
 * Created by jasontesser on 9/30/16.
 */
public interface Parentable {


    public default boolean isParent() {
        return true;
    }

    public String getParentId();
    
    
    
}
