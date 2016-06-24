package com.dotmarketing.util;

import com.dotcms.repackage.edu.emory.mathcs.backport.java.util.Arrays;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

// todo: unit test
/**
 * Just encapsulates collection utils methods
 * @author jsanca
 */
public class CollectionsUtils implements Serializable {

    /**
     * Get a new empty list
     * @param <T>
     * @return List
     */
    public static <T> List<T> getNewList() {

        return new ArrayList<T>();
    } // getNewList

    /**
     * Get a new empty list
     * @param <T>
     * @return List
     */
    public static <T> List<T> getNewList(T... elements) {

        return getNewList(Arrays.asList(elements));
    } // getNewList

    /**
     * Get a new empty list
     * @param <T>
     * @return List
     */
    public static <T> List<T> getNewList(Collection<T> tCollection) {

        return new ArrayList<T>(tCollection);
    } // getNewList

} // E:O:F:CollectionsUtils.
