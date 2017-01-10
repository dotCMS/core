package com.dotcms.unittest;

import java.util.List;

/**
 * @author Geoff M. Granum
 */
public class TestUtil {

    public static <T> Object[][] toCaseArray(List<T> list){
        Object[][] ary = new Object[list.size()][];
        for (int i = 0; i < list.size(); i++) {
            ary[i] = new Object[]{list.get(i)};
        }
        return ary;
    }
}
 
