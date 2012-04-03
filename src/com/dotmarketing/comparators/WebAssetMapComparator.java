package com.dotmarketing.comparators;

import java.util.Comparator;
import java.util.Date;
import java.util.Map;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

/**
 * @author David
 */
public class WebAssetMapComparator implements Comparator<Map<String, Object>> {

    private String orderField = "name";
    private int orderDirection = 1;

    public WebAssetMapComparator(String field, boolean orderDescending) {
        super();
        if (orderDescending)
        	this.orderDirection = -1;
        this.orderField = field;
    }

    public int compare(Map map1, Map map2) {

    	if(orderField.equals("name"))
    	{
    		if(map1.get("type").equals("folder") && map2.get("type").equals("folder")) {
    			return orderDirection * ((String)map1.get("name")).compareTo((String)map2.get("name"));
    		} else if (map1.get("type").equals("folder")) {
    			return -orderDirection;
    		} else if (map2.get("type").equals("folder")) {
    			return orderDirection;
    		}
    	}

      	Object c1 = map1.get(orderField);
       	Object c2 = map2.get(orderField);
       	if (orderField.equals("name")) {
       		if (map1.get("type").equals("htmlpage"))
       			c1 = map1.get("pageUrl");
       		else if (map1.get("type").equals("file_asset"))
       			c1 = map1.get("fileName");
       		else if (map1.get("type").equals("link"))
       			c1 = map1.get("title");

       		if (map2.get("type").equals("htmlpage"))
       			c2 = map2.get("pageUrl");
       		else if (map2.get("type").equals("file_asset"))
       			c2 = map2.get("fileName");
       		else if (map2.get("type").equals("link"))
       			c2 = map2.get("title");
       	}
       	if (c1 != null && c2 != null) {
       		if (orderField.equals("name")) {
       			return orderDirection * ((String)c1).compareTo((String)c2);
       		} else if (orderField.equals("modUser")) {
       			String userId1 = (String)c1;
       			String userId2 = (String)c2;
       			User user1 = null;
       			User user2 = null;
       			try{
	       			user1 = APILocator.getUserAPI().loadUserById(userId1,APILocator.getUserAPI().getSystemUser(),false);
	       			user2 = APILocator.getUserAPI().loadUserById(userId2,APILocator.getUserAPI().getSystemUser(),false);
       			}catch (Exception e) {
					Logger.error(this, e.getMessage(), e);
       				return 0;
				}
       			return orderDirection * (user1.getFullName()).compareTo(user2.getFullName());
       		} else if (orderField.equals("modDate")) {
       			return orderDirection * ((Date)c1).compareTo((Date)c2);
       		} else if (orderField.equals("sortOrder")) {
                int result = 0;
                long c1long = 0;
                long c2long = 0;
                if(c1 instanceof Long){
                    c1long = (long)((Long)c1);                     
                    c1long = (c1long == 0 ? Integer.MAX_VALUE : c1long);
                }else if(c1 instanceof Integer){
                    c1long = (int)((Integer)c1);                     
                    c1long = (c1long == 0 ? Integer.MAX_VALUE : c1long);
                }
                if(c2 instanceof Long){                     
                    c2long = (long)((Long)c2);                     
                    c2long = (c2long == 0 ? Integer.MAX_VALUE : c2long);
                }else if(c2 instanceof Integer){                     
                    c2long = (int)((Integer)c2);                     
                    c2long = (c2long == 0 ? Integer.MAX_VALUE : c2long);
                }
                result = (c1long) > (c2long)? 1 : (c1long) < (c2long)? -1 : 0 ;
                return orderDirection * result;
					
	

       		}
       	}
        return 0;
    }
}