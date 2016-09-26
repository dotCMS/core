package com.dotcms.rest.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.dotcms.repackage.javax.ws.rs.NameBinding;
/**
Use: 
	@NoCache
	public Response indexCount ( @Context HttpServletRequest request, @PathParam ("query") String query,
			@PathParam ("type") String type,
			@PathParam ("callback") String callback ) throws DotDataException, DotSecurityException {


		ResourceResponse responseResource = new ResourceResponse( paramsMap ); 

		return responseResource.response( Long.toString( APILocator.getContentletAPI().indexCount( query, initData.getUser(), true ) ) );
	}

*/
@NameBinding
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(value = RetentionPolicy.RUNTIME)
public @interface NoCache {
	String cc() default "public, must-revalidate";
}

