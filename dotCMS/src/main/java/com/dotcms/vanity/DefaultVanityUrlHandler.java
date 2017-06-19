package com.dotcms.vanity;

import com.dotcms.content.model.VanityUrl;
import com.dotmarketing.beans.Host;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.filters.CMSFilter;
import com.dotmarketing.filters.CmsUrlUtil;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.UtilMethods;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * This class implements the methods defined in the {@link VanityUrlHandler}
 * Created by oswaldogallango on 2017-06-16.
 */
public class DefaultVanityUrlHandler implements VanityUrlHandler{

    CmsUrlUtil urlUtil = CmsUrlUtil.getInstance();

    @Override
    public VanityUrlResult handle(VanityUrl vanityUrl, HttpServletResponse response, Host host, long languageId) throws IOException{
        String rewrite = null;
        String queryString = null;
        CMSFilter.IAm iAm = null;

        if(vanityUrl != null){
            rewrite = InodeUtils.isSet(vanityUrl.getInode())?vanityUrl.getForwardTo():null;

            if(vanityUrl.getAction() == HttpServletResponse.SC_OK){
                //then forward
                response.setStatus(vanityUrl.getAction());
            }else if(vanityUrl.getAction() == HttpServletResponse.SC_MOVED_PERMANENTLY || vanityUrl.getAction() == HttpServletResponse.SC_FOUND){
                //redirect
                response.setStatus(vanityUrl.getAction());
                response.sendRedirect(rewrite);

                closeDbSilently();
                return new VanityUrlResult(null,null,null,true);
            }else{
                //errors
                response.sendError(vanityUrl.getAction());

                closeDbSilently();
                return new VanityUrlResult(null,null,null,true);
            }
        }
        if (UtilMethods.isSet(rewrite) && rewrite.contains("//")) {
            response.sendRedirect(rewrite);

            closeDbSilently();
            return new VanityUrlResult(null,null,null,true);
        }
        if (UtilMethods.isSet(rewrite)) {
            if(rewrite!=null && rewrite.contains("?")){
                String[] arr = rewrite.split("\\?",2);
                rewrite = arr[0];
                if(arr.length>1){
                    queryString= arr[1];
                }
            }
            if (urlUtil.isFileAsset(rewrite, host, languageId)) {
                iAm= CMSFilter.IAm.FILE;
            } else if (urlUtil.isPageAsset(rewrite, host, languageId)) {
                iAm = CMSFilter.IAm.PAGE;
            } else if (urlUtil.isFolder(rewrite, host)) {
                iAm = CMSFilter.IAm.FOLDER;
            }
        }
        return new VanityUrlResult(rewrite, queryString,iAm,false);
    }

    /**
     * Close the Hibernate session and DB connection
     */
    private void closeDbSilently() {
        try {
            HibernateUtil.closeSession();
        } catch (Exception e) {

        } finally {
            try {

                DbConnectionFactory.closeConnection();
            } catch (Exception e) {

            }
        }
    }
}
