package com.dotcms.rest.api;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.inject.Named;

@Named("helloAppScoped")
//@Default
@ApplicationScoped
public class HelloWorldImpl implements SayHelloBean {

    public String sayHello() {
        return "Hello, World!";
    }

}
