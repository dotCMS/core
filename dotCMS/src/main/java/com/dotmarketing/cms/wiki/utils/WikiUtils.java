package com.dotmarketing.cms.wiki.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import com.dotmarketing.util.Logger;
import com.liferay.util.StringUtil;

public class WikiUtils {

    
    
    public static String normalizeTitle(String title){
        if(title ==null)return null;
        String newTitle = title;
        try {
            newTitle = URLDecoder.decode(title, "UTF-8");
        } catch (UnsupportedEncodingException e) {
           Logger.error(WikiUtils.class,"UnsupportedEncodingException");
        }
        newTitle = newTitle.replace("?", " ");
        newTitle = StringUtil.trimLeading(newTitle);
        newTitle = StringUtil.trimTrailing(newTitle);
        newTitle = newTitle.toLowerCase();
        do{
            newTitle = newTitle.replaceAll("  ", " ");
        } while(newTitle.indexOf("  ") > -1);
        
        return newTitle;
        
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
}
