package com.dotcms.rest.api;


import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Alternative;
import javax.inject.Named;

@Named("helloDependant")
@Dependent
//@Alternative
public class HelloDependantImpl implements SayHelloBean {

    @Override
    public String sayHello() {
        return "Hello form a dependant bean!";
    }

}
