package com.dotcms.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.Constants;

public class DownloadUtil {
    private DownloadUtil() {} 
    
    public static class ThreadLocalHTTPDate extends ThreadLocal<java.text.SimpleDateFormat>{
        @Override
        protected SimpleDateFormat initialValue() {
            SimpleDateFormat sdf = new java.text.SimpleDateFormat(Constants.RFC2822_FORMAT);
            sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
            return sdf;
        }
    }
    
    public static final ThreadLocalHTTPDate httpDate = new DownloadUtil.ThreadLocalHTTPDate();
    
    public static boolean isModifiedEtag(HttpServletRequest request, HttpServletResponse response,String assetId, long _lastModified, long fileSize) {
        int _daysCache = Config.getIntProperty("asset.cache.control.max.days", 30);
        GregorianCalendar expiration = new GregorianCalendar();
        expiration.add(java.util.Calendar.DAY_OF_MONTH, _daysCache);
        int seconds = (_daysCache * 24 * 60 * 60);

        if(_lastModified < 0) {
            _lastModified = 0;
        }
        // we need to round the _lastmodified to get rid of the milliseconds.
        _lastModified = _lastModified / 1000;
        _lastModified = _lastModified * 1000;
        Date _lastModifiedDate = new java.util.Date(_lastModified);

        String _eTag = "dot:" + assetId + ":" + _lastModified + ":" + fileSize;

        /* Setting cache friendly headers */
        response.setHeader("Expires", httpDate.get().format(expiration.getTime()));
        response.setHeader("Cache-Control", "public, max-age="+seconds);


        String ifModifiedSince = request.getHeader("If-Modified-Since");
        String ifNoneMatch = request.getHeader("If-None-Match");

        /*
         * If the etag matches then the file is the same
         *
        */
        if(ifNoneMatch != null){
            if(_eTag.equals(ifNoneMatch) || ifNoneMatch.equals("*")){
                response.setStatus(HttpServletResponse.SC_NOT_MODIFIED );
                return false;
            }
        }

        /* Using the If-Modified-Since Header */
         if(ifModifiedSince != null){
            try{
                Date ifModifiedSinceDate = httpDate.get().parse(ifModifiedSince);

                if(_lastModifiedDate.getTime() <= ifModifiedSinceDate.getTime()){

                    response.setStatus(HttpServletResponse.SC_NOT_MODIFIED );
                    return false;
                }
            }
            catch(Exception e){}

        }

        response.setHeader("Last-Modified", httpDate.get().format(_lastModifiedDate));
        response.setHeader("ETag", _eTag);
        
        return true;
    }
}
