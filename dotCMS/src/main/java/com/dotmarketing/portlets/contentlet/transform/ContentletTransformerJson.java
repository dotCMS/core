package com.dotmarketing.portlets.contentlet.transform;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import java.util.HashMap;
import java.util.Map;

public class ContentletTransformerJson {

    private final Contentlet contentlet;

    public ContentletTransformerJson(final Contentlet contentlet) {
        this.contentlet = contentlet;
    }

    public Map<String,Object> toMap(){
       return contentletToMap(contentlet);
    }

    private Map<String, Object> contentletToMap(final Contentlet contentlet) {
        User modUser = null;
        try {
            modUser = APILocator.getUserAPI().loadUserById(contentlet.getModUser());
        } catch (Exception e) {
            Logger.error(getClass(),"Error loading user from db. ", e);
        }
        final Map<String, Object> conMap = new HashMap<>();
        conMap.put("title", contentlet.getTitle());
        conMap.put("modDate", UtilMethods.dateToJDBC(contentlet.getModDate()));
        conMap.put("language", contentlet.getLanguageId());
        conMap.put("inode", contentlet.getInode());
        conMap.put("modUser", contentlet.getModUser());
        conMap.put("modUserName",  null == modUser ? "N/A" : modUser.getFullName());
        conMap.put("folder", contentlet.getFolder());
        conMap.put("host", contentlet.getHost());
        return conMap;
    }

}
