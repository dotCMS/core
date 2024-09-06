package com.dotcms.cdi;

import javax.enterprise.context.Dependent;

@Dependent
public class MessageServiceBean {

    public String getMessage() {
        return "Hello World";
    }

}
