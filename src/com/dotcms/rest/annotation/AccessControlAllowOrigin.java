package com.dotcms.rest.annotation;

import com.dotcms.repackage.javax.ws.rs.NameBinding;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * To send this header: Access-Control-Allow-Origin: *
 * Use:
 * <code>
 *     <pre>
 *     @AccessControlAllowOrigin
 *     public Response indexCount ( @Context HttpServletRequest request, @PathParam ("query") String query,
 *      @PathParam ("type") String type,
 *      @PathParam ("callback") String callback ) throws DotDataException, DotSecurityException {
 *                  ...
 *      }
 *     </pre>
 *
 *     optional there is a value parameter, by default it is: *
 * </code>
 */
@NameBinding
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(value = RetentionPolicy.RUNTIME)
public @interface AccessControlAllowOrigin {

    /**
     * Value for the header
     * @return String
     */
    String value() default "*";
} // E:O:F:AccessControlAllowOrigin
