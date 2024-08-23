package com.dotcms.cdi;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SayHelloBean {

    public String sayHello() {
        return "Hello";
    }

}
