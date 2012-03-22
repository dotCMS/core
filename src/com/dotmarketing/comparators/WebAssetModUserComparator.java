package com.dotmarketing.comparators;

import java.util.Comparator;

import com.dotmarketing.beans.WebAsset;

/**
 * @author Maria
 * 
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates. To enable and disable the creation of type
 * comments go to Window>Preferences>Java>Code Generation.
 */
public class WebAssetModUserComparator implements Comparator {

    private String orderType = "";

    public WebAssetModUserComparator(String orderType) {
        super();
        this.orderType = orderType;
    }

    public int compare(Object o1, Object o2) {

        try {
            WebAsset w1 = (WebAsset) o1;
            WebAsset w2 = (WebAsset) o2;

            int ret = w1.getTitle().compareTo(w2.getTitle());

            if (orderType.equals("asc")) {
                return ret;
            }

            return ret * -1;

        } catch (ClassCastException e) {

        }
        return 0;
    }
}