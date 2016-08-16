package com.dotcms.util;


import com.liferay.portal.model.User;

import java.util.Date;

public class UserUtilTest {

    public static User createUser(){
        User user = new User();
        user.setActive( true );
        user.setCompanyId("company");
        user.setBirthday( new Date() );
        user.setComments( "comments" );
        user.setCreateDate( new Date() );
        user.setModificationDate( new Date() );
        user.setEmailAddress( "a@a.com" );
        user.setFailedLoginAttempts( 1 );
        user.setMale( true );
        user.setFirstName( "firstName" );
        user.setLanguageId("1");
        user.setLoginDate( new Date() );
        user.setLastLoginIP( "127.0.0.1" );
        user.setLastName( "lastName" );
        user.setMiddleName( "middleName" );
        user.setFemale( false );
        user.setNickName( "nickName" );
        user.setUserId("dotcms.1");

        return user;
    }
}
