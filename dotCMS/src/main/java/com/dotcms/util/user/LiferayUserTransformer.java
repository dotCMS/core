package com.dotcms.util.user;

import com.dotmarketing.business.DotStateException;
import com.dotmarketing.db.DbConnectionFactory;
import com.liferay.portal.ejb.UserPool;
import com.liferay.portal.model.User;
import java.util.Map;

/**
 * This class implement the UserTransformer interface
 * to convert dotconnect maps from user to User
 */
public class LiferayUserTransformer implements UserTransformer {


    @Override
    public User fromMap(Map<String, Object> map) throws DotStateException {
        User user = null;
        if(null != map) {
            user = new com.liferay.portal.model.User();
            user.setUserId((String) map.get("userid"));
            user.setCompanyId((String) map.get("companyid"));
            user.setCreateDate((java.util.Date) map.get("createdate"));
            user.setModificationDate((java.util.Date) map.get("mod_date"));
            user.setPassword((String) map.get("password_"));
            user.setPasswordEncrypted(
                    DbConnectionFactory.isDBTrue(map.get("passwordencrypted").toString()));
            user.setPasswordExpirationDate((java.util.Date) map.get("mod_date"));
            user.setPasswordReset(
                    DbConnectionFactory.isDBTrue(map.get("passwordencrypted").toString()));
            user.setFirstName((String) map.get("firstname"));
            user.setMiddleName((String) map.get("middlename"));
            user.setLastName((String) map.get("lastname"));
            user.setNickName((String) map.get("nickname"));
            user.setMale(DbConnectionFactory.isDBTrue(map.get("male").toString()));
            user.setModificationDate((java.util.Date) map.get("birthday"));
            user.setEmailAddress((String) map.get("emailaddress"));
            user.setSmsId((String) map.get("smsid"));
            user.setAimId((String) map.get("aimid"));
            user.setIcqId((String) map.get("icqid"));
            user.setMsnId((String) map.get("msnid"));
            user.setYmId((String) map.get("ymid"));
            user.setFavoriteActivity((String) map.get("favoriteactivity"));
            user.setFavoriteBibleVerse((String) map.get("favoritebibleverse"));
            user.setFavoriteFood((String) map.get("favoritefood"));
            user.setFavoriteMovie((String) map.get("favoritemovie"));
            user.setFavoriteMusic((String) map.get("favoritemusic"));
            user.setLanguageId((String) map.get("languageid"));
            user.setTimeZoneId((String) map.get("timezoneid"));
            user.setSkinId((String) map.get("skinid"));
            user.setDottedSkins(DbConnectionFactory.isDBTrue(map.get("dottedskins").toString()));
            user.setRoundedSkins(DbConnectionFactory.isDBTrue(map.get("roundedskins").toString()));
            user.setGreeting((String) map.get("greeting"));
            user.setResolution((String) map.get("resolution"));
            user.setRefreshRate((String) map.get("refreshrate"));
            user.setLayoutIds((String) map.get("layoutids"));
            user.setComments((String) map.get("comments"));
            user.setLoginDate((java.util.Date) map.get("logindate"));
            user.setLoginIP((String) map.get("loginip"));
            user.setLastLoginDate((java.util.Date) map.get("lastlogindate"));
            user.setLastLoginIP((String) map.get("lastloginip"));
            user.setFailedLoginAttempts((Integer) map.get("failedloginattempts"));
            user.setAgreedToTermsOfUse(DbConnectionFactory.isDBTrue(map.get("agreedtotermsofuse").toString()));
            user.setActive(DbConnectionFactory.isDBTrue(map.get("active_").toString()));
            if(null != map.get("delete_in_progress")) {
                user.setDeleteInProgress(DbConnectionFactory.isDBTrue(map.get("delete_in_progress")
                                .toString()));
            }
            user.setDeleteDate((java.util.Date) map.get("mod_date"));

            UserPool.put(user.getPrimaryKey(), user);
        }

        return user;
    }
}
