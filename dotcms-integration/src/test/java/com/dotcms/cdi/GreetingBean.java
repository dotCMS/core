package com.dotcms.cdi;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;


@ApplicationScoped
public class GreetingBean {

    @Inject
    MessageServiceBean injectedTestBean;

    public String greet() {
        return injectedTestBean.getMessage();
    }

}
