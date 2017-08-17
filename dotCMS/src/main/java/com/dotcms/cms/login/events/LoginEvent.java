package com.dotcms.cms.login.events;

import java.util.EventObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LoginEvent extends EventObject {


    private static final long serialVersionUID = 1L;


    
    
    public final String userId;
    public final String password;
    public final HttpServletResponse response;
    public final HttpServletRequest request;
    public final LoginEvents event;
    public LoginEvent(Object source, LoginEvents event, String userId, String password, HttpServletRequest request,
                    HttpServletResponse response) {
        super(source);
        this.userId = userId;
        this.password = password;
        this.response = response;
        this.request = request;
        this.event = event;
    }

}
