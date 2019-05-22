package com.dotcms.rest.annotation;

import javax.ws.rs.NameBinding;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Add this annotation if you are returning as an entity on the {@link com.dotcms.rest.ResponseEntityView} a {@link com.dotmarketing.business.Permissionable}
 * and wants to populate the ResponseEntityView.permissions automatically,
 * Note: we avoid the READ since if you are getting the Permissions it means you can read it.
 * By default the request will need a query string called include_permissions=true in order to include the permissions on the response or not.
 * However you can change it by queryParam (optional)
 *
 */
@NameBinding
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(value = RetentionPolicy.RUNTIME)
public @interface IncludePermissions {
    String queryParam() default "include_permissions";
}

