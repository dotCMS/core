package com.dotcms.rest.api;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

// @ApplicationScoped
// In this case, this annotation is not needed
// Because what dictates the scope of the bean is the scope of the method that produces it
public class ProducedHelloBeanImpl implements SayHelloBean {

    @Override
    public String sayHello() {
        return "Hello from a produced bean!";
    }

    @Produces
    @ApplicationScoped // if this wasn't here, the bean would be @Dependent
    @ProducedBeanExample
    SayHelloBean producedBean() {
        // any additional injection needs to take place here
        return new ProducedHelloBeanImpl();
    }

}
