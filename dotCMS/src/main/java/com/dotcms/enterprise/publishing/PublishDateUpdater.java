package com.dotcms.enterprise.publishing;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class PublishDateUpdater {

    final static String toPublish="select working_inode from identifier join contentlet_version_info " +
        " on (identifier.id=contentlet_version_info.identifier) " +
        " where syspublish_date is not null and syspublish_date<=? " +
        " and (sysexpire_date is null or sysexpire_date >= ?) " + 
        " and (live_inode is null or live_inode<>working_inode) "; 
    
    final static String toExpire="select id,lang from identifier join contentlet_version_info " +
        " on (identifier.id=contentlet_version_info.identifier) " +
        " where sysexpire_date is not null and sysexpire_date<=? " +
        " and live_inode is not null";

    /**
     * Update the expire date with a fire time given as a parameter.
     * @param fireTime
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @WrapInTransaction
    public static void updatePublishExpireDates(final Date fireTime) throws DotDataException, DotSecurityException {

	    if(LicenseUtil.getLevel()< LicenseLevel.PROFESSIONAL.level){
	        return;
	    }

        final User systemUser       = APILocator.getUserAPI().getSystemUser();
        final DotConnect dotConnect = new DotConnect();
        dotConnect.setSQL(toPublish);
        dotConnect.addParam(fireTime);
        dotConnect.addParam(fireTime);

        for(final Map<String,Object> contentletRow : (List<Map<String,Object>>)dotConnect.loadResults()) {

            try{

                final Contentlet contentlet = APILocator.getContentletAPI().find
                        ((String)contentletRow.get("working_inode"), systemUser, false);
                APILocator.getContentletAPI().publish(contentlet, locker(contentlet), false);
            }
            catch(Exception e){
                Logger.debug(PublishDateUpdater.class, "content failed to publish: " +  e.getMessage());
            }
        }

        dotConnect.setSQL(toExpire);
        dotConnect.addParam(fireTime);

        for(final Map<String,Object> contentletRow : (List<Map<String,Object>>)dotConnect.loadResults()) {

            final long lang = contentletRow.get("lang") instanceof String ?
                    Long.parseLong((String)contentletRow.get("lang")) : ((Number)contentletRow.get("lang")).longValue();
            try {

                final Contentlet contentlet = APILocator.getContentletAPI().findContentletByIdentifier((String)contentletRow.get("id"), true, lang, systemUser, false);
                APILocator.getContentletAPI().unpublish(contentlet, locker(contentlet), false);
            }
            catch(Exception e){
                Logger.debug(PublishDateUpdater.class, "content failed to publish: " +  e.getMessage());
            }
        }
    }

    /**
     *
     * @param contentlet
     * @return
     * @throws DotDataException
     */
    private static User locker(final Contentlet contentlet) throws DotDataException {

        User locker = APILocator.getUserAPI().getSystemUser();
        try {
            User modUser = APILocator.getUserAPI()
                    .loadUserById(contentlet.getModUser(), locker, false);
            if (APILocator.getContentletAPI().canLock(contentlet, locker)) {
                locker = modUser;
            }
        } catch (Exception userEx) {
            Logger.error(PublishDateUpdater.class, userEx.getMessage(), userEx);
        }

        return locker;
    }

}
