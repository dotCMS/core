package com.dotmarketing.comparators;

import java.util.Comparator;

import com.dotmarketing.beans.WebAsset;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

/**
 * @author Maria
 * 
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates. To enable and disable the creation of type
 * comments go to Window>Preferences>Java>Code Generation.
 */
public class WebAssetTitleComparator implements Comparator {

    private String orderType = "";

    public WebAssetTitleComparator(String orderType) {
        super();
        this.orderType = orderType;
    }

    public int compare(Object o1, Object o2) {

        try {
            WebAsset w1 = (WebAsset) o1;
            WebAsset w2 = (WebAsset) o2;

            User user1 = APILocator.getUserAPI().loadUserById(w1.getModUser(),APILocator.getUserAPI().getSystemUser(),false);
            User user2 = APILocator.getUserAPI().loadUserById(w2.getModUser(),APILocator.getUserAPI().getSystemUser(),false);
            
            int ret = user1.getFullName().compareTo(user2.getFullName());

            if (orderType.equals("asc")) {
                return ret;
            }

            return ret * -1;

        } catch (ClassCastException e) {

        }catch (Exception ex) {
        	Logger.error(this, ex.getMessage(), ex);
        }
        return 0;
    }
}