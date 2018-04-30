package com.dotcms.rest.annotation;

import com.dotcms.repackage.javax.ws.rs.NameBinding;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Add this annotation if you are returning as an entity on the {@link com.dotcms.rest.ResponseEntityView} a {@link com.dotmarketing.business.Permissionable}
 * and wants a header called permissionable, such as
 * <i>header:permissionable: [WRITE, PUBLISH, EDITPERMISSION]</i> that are the permissions that the request user has.
 * Note: we avoid the READ since if you are getting the Permissions it means you can read it.
 * By default the request will need a query string called permissionable=true in order to include the header or not.
 * However you can change it by queryParam (optional)
 *
 */
@NameBinding
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(value = RetentionPolicy.RUNTIME)
public @interface Permissions {
    String queryParam() default "permissionable";
}

