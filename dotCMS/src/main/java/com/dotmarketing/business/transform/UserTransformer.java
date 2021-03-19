package com.dotmarketing.business.transform;

import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotcms.util.transform.DBTransformer;
import com.dotmarketing.db.DbConnectionFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.postgresql.util.PGobject;

public class UserTransformer implements DBTransformer<User> {
    final List<User> list;

    private static final ObjectMapper mapper = DotObjectMapperProvider.getInstance()
            .getDefaultObjectMapper();


    public UserTransformer(List<Map<String, Object>> initList){
        List<User> newList = new ArrayList<>();
        if (initList != null){
            for(Map<String, Object> map : initList){
                newList.add(transform(map));
            }
        }

        this.list = newList;
    }

    @Override
    public List<User> asList() {

        return new ArrayList<>(this.list);
    }

    @Override
    public User findFirst() {
        return this.asList().stream().findFirst().orElse(new User());
    }

    @NotNull
    private static User transform(Map<String, Object> map)  {
        final User user = new User();
        user.setUserId((String) map.get("userid"));
        user.setCompanyId((String) map.get("companyid"));
        user.setCreateDate((Date) map.get("createdate"));
        user.setModificationDate((Date) map.get("mod_date"));
        user.setPassword((String) map.get("password_"));

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

        user.setFirstName((String) map.get("firstname"));

        user.setMiddleName((String) map.get("middlename"));
        user.setLastName((String) map.get("lastname"));
        user.setNickName((String) map.get("nickname"));

        if (null != map.get("male")) {
            user.setMale(DbConnectionFactory.isDBTrue(map.get("male").toString()));
        }

        user.setBirthday((Date) map.get("birthday"));
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

        if (null != map.get("dottedskins")) {
            user.setDottedSkins(
                    DbConnectionFactory.isDBTrue(map.get("dottedskins").toString()));
        }

        if (null != map.get("roundedskins")) {
            user.setRoundedSkins(
                    DbConnectionFactory.isDBTrue(map.get("roundedskins").toString()));
        }

        user.setGreeting((String) map.get("greeting"));
        user.setResolution((String) map.get("resolution"));
        user.setRefreshRate((String) map.get("refreshrate"));
        user.setLayoutIds((String) map.get("layoutids"));
        user.setComments((String) map.get("comments"));
        user.setLoginDate((Date) map.get("logindate"));
        user.setLoginIP((String) map.get("loginip"));
        user.setLastLoginDate((Date) map.get("lastlogindate"));
        user.setLastLoginIP((String) map.get("lastloginip"));

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

        user.setDeleteDate((Date) map.get("delete_date"));

        if (null != map.get("additional_info")) {
            user.setAdditionalInfo(Try.of(() -> mapper
                    .readValue(DbConnectionFactory.isPostgres() ? ((PGobject) map
                            .get("additional_info")).getValue()
                            : (String) map.get("additional_info"), HashMap.class))
                    .getOrElse(new HashMap<String, String>()));
        }
        return user;
    }
}
