package com.dotcms.util.user;

import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotmarketing.db.DbConnectionFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import org.postgresql.util.PGobject;

import java.util.HashMap;
import java.util.Map;

/**
 * This class implement the UserTransformer interface to convert dotconnect maps from user to User
 * @deprecated User {@link com.dotmarketing.business.transform.UserTransformer} instead
 */
@Deprecated
public class LiferayUserTransformer {

    public User fromMap(final Map<String, Object> map) {
        User user = null;
        if (null != map) {
            user = new User();
            if (null != map.get("userid")) {
                user.setUserId((String) map.get("userid"));
            }
            if (null != map.get("companyid")) {
                user.setCompanyId((String) map.get("companyid"));
            }
            if (null != map.get("createdate")) {
                user.setCreateDate((java.util.Date) map.get("createdate"));
            }
            if (null != map.get("mod_date")) {
                user.setModificationDate((java.util.Date) map.get("mod_date"));
            }
            if (null != map.get("password_")) {
                user.setPassword((String) map.get("password_"));
            }
            if (null != map.get("passwordencrypted")) {
                user.setPasswordEncrypted(
                        DbConnectionFactory.isDBTrue(map.get("passwordencrypted").toString()));
            }
            if (null != map.get("passwordexpirationddate")) {
                user.setPasswordExpirationDate((java.util.Date) map.get("passwordexpirationddate"));
            }
            if (null != map.get("passwordreset")) {
                user.setPasswordReset(
                        DbConnectionFactory.isDBTrue(map.get("passwordreset").toString()));
            }
            if (null != map.get("firstname")) {
                user.setFirstName((String) map.get("firstname"));
            }
            if (null != map.get("middlename")) {
                user.setMiddleName((String) map.get("middlename"));
            }
            if (null != map.get("lastname")) {
                user.setLastName((String) map.get("lastname"));
            }
            if (null != map.get("nickname")) {
                user.setNickName((String) map.get("nickname"));
            }
            if (null != map.get("male")) {
                user.setMale(DbConnectionFactory.isDBTrue(map.get("male").toString()));
            }
            if (null != map.get("birthday")) {
                user.setModificationDate((java.util.Date) map.get("birthday"));
            }
            if (null != map.get("emailaddress")) {
                user.setEmailAddress((String) map.get("emailaddress"));
            }
            if (null != map.get("smsid")) {
                user.setSmsId((String) map.get("smsid"));
            }
            if (null != map.get("aimid")) {
                user.setAimId((String) map.get("aimid"));
            }
            if (null != map.get("icqid")) {
                user.setIcqId((String) map.get("icqid"));
            }
            if (null != map.get("msnid")) {
                user.setMsnId((String) map.get("msnid"));
            }
            if (null != map.get("ymid")) {
                user.setYmId((String) map.get("ymid"));
            }
            if (null != map.get("favoriteactivity")) {
                user.setFavoriteActivity((String) map.get("favoriteactivity"));
            }
            if (null != map.get("favoritebibleverse")) {
                user.setFavoriteBibleVerse((String) map.get("favoritebibleverse"));
            }
            if (null != map.get("favoritefood")) {
                user.setFavoriteFood((String) map.get("favoritefood"));
            }
            if (null != map.get("favoritemovie")) {
                user.setFavoriteMovie((String) map.get("favoritemovie"));
            }
            if (null != map.get("favoritemusic")) {
                user.setFavoriteMusic((String) map.get("favoritemusic"));
            }
            if (null != map.get("languageid")) {
                user.setLanguageId((String) map.get("languageid"));
            }
            if (null != map.get("timezoneid")) {
                user.setTimeZoneId((String) map.get("timezoneid"));
            }
            if (null != map.get("skinid")) {
                user.setSkinId((String) map.get("skinid"));
            }
            if (null != map.get("dottedskins")) {
                user.setDottedSkins(
                        DbConnectionFactory.isDBTrue(map.get("dottedskins").toString()));
            }
            if (null != map.get("roundedskins")) {
                user.setRoundedSkins(
                        DbConnectionFactory.isDBTrue(map.get("roundedskins").toString()));
            }
            if (null != map.get("greeting")) {
                user.setGreeting((String) map.get("greeting"));
            }
            if (null != map.get("resolution")) {
                user.setResolution((String) map.get("resolution"));
            }
            if (null != map.get("refreshrate")) {
                user.setRefreshRate((String) map.get("refreshrate"));
            }
            if (null != map.get("layoutids")) {
                user.setLayoutIds((String) map.get("layoutids"));
            }
            if (null != map.get("comments")) {
                user.setComments((String) map.get("comments"));
            }
            if (null != map.get("logindate")) {
                user.setLoginDate((java.util.Date) map.get("logindate"));
            }
            if (null != map.get("loginip")) {
                user.setLoginIP((String) map.get("loginip"));
            }
            if (null != map.get("lastlogindate")) {
                user.setLastLoginDate((java.util.Date) map.get("lastlogindate"));
            }
            if (null != map.get("lastloginip")) {
                user.setLastLoginIP((String) map.get("lastloginip"));
            }
            if (null != map.get("failedloginattempts")) {
                user.setFailedLoginAttempts(Integer.parseInt(map.get("failedloginattempts").toString()));
            }
            if (null != map.get("agreedtotermsofuse")) {
                user.setAgreedToTermsOfUse(
                        DbConnectionFactory.isDBTrue(map.get("agreedtotermsofuse").toString()));
            }
            if (null != map.get("active_")) {
                user.setActive(DbConnectionFactory.isDBTrue(map.get("active_").toString()));
            }
            if (null != map.get("delete_in_progress")) {
                user.setDeleteInProgress(DbConnectionFactory.isDBTrue(map.get("delete_in_progress")
                        .toString()));
            }
            if (null != map.get("delete_date")) {
                user.setDeleteDate((java.util.Date) map.get("delete_date"));
            }
            if (null != map.get("additional_info")) {
                Object additionalInfoObject = map.get("additional_info");

                if (additionalInfoObject instanceof PGobject) {
                    PGobject pgObject = (PGobject) additionalInfoObject;
                    String jsonString = pgObject.getValue();
                    user.setAdditionalInfo(
                            Try.of(() -> DotObjectMapperProvider.getInstance().getDefaultObjectMapper()
                                            .readValue(jsonString, HashMap.class))
                                    .getOrElse(new HashMap<>())
                    );
                } else if (additionalInfoObject instanceof String) {
                    user.setAdditionalInfo(
                            Try.of(() -> DotObjectMapperProvider.getInstance().getDefaultObjectMapper()
                                            .readValue((String) additionalInfoObject, HashMap.class))
                                    .getOrElse(new HashMap<>())
                    );
                } else {
                    // Fallback in case of an unexpected type
                    user.setAdditionalInfo(new HashMap<>());
                }
            }
        }

        return user;
    }
}
