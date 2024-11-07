package com.dotcms.rest.api;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import java.util.Set;
import java.util.stream.Collectors;
import javax.ws.rs.core.MediaType;

@Path("/test")
public class RegisterResource {

    @Context
    Application application;

    final SayHelloBean appScopedBean;

    final SayHelloBean depScopedBean;

    final SayHelloBean producedBean;

    @Inject
    public RegisterResource(@Named("helloAppScoped") SayHelloBean appScopedBean,
                            @Named("helloDependant") SayHelloBean depScopedBean,
                            @ProducedBeanExample SayHelloBean producedBean) {
        this.appScopedBean = appScopedBean;
        this.depScopedBean = depScopedBean;
        this.producedBean = producedBean;
    }

    @GET
    @Path ("/")
    @Produces(MediaType.APPLICATION_JSON)
    public Set<String> getRegisteredResources() {

        return application.getClasses().stream()
                .map(Class::getName)
                .collect(Collectors.toSet());
    }

    @GET
    @Path ("/echo")
    @Produces(MediaType.APPLICATION_JSON)
    public Set<String> echo() {
        return Set.of(
                "Application Scope Bean: " + appScopedBean.sayHello(),
                "Dependant Scope Bean: " + depScopedBean.sayHello(),
                "Produced Bean: " + producedBean.sayHello());
    }
}
