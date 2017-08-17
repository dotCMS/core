package com.dotcms.cms.login.events;


public class LDAPLoginEventListener implements LoginEventListener {

    @Override
    public void loginEventReceived(LoginEvent event) {
        switch(event.event){
            case PRE_LOGIN:
                System.out.println("PRE_LOGIN Listened to");
                break;
            case POST_LOGIN_SUCCESS:
                System.out.println("POST_LOGIN_SUCCESS Listened to");
                break;
            case POST_LOGIN_FAILURE:
                System.out.println("POST_LOGIN_FAILURE Listened to");
                break;
            case PRE_LOGOUT:
                System.out.println("PRE_LOGOUT Listened to");
                break;
                
            case POST_LOGOUT:
                System.out.println("POST_LOGOUT Listened to");
                break;
        }


    }

}
