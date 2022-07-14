package com.dotcms.model.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
//import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;


@Retention(RetentionPolicy.CLASS)
@Target(ElementType.PACKAGE)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public @interface RestClient {

}
