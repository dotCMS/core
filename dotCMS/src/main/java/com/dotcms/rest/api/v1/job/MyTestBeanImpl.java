package com.dotcms.rest.api.v1.job;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MyTestBeanImpl implements MyTestBean {

    public String sayHello() {
        return "Hello, World!";
    }

}
