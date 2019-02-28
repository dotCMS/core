package com.dotcms.business;

import java.lang.annotation.*;

/**
 * Method annotated with this annotation can set a parameter decorator to do something over the
 * parameters before invoke, such as interceptor.
 *
 * @author jsanca
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface MethodHook {

    /**
     * Pre hook
     * @return Class
     */
    Class<? extends PreHook>  preHook  () default PreHook.class;

    /**
     * Post hook
     * @return
     */
    Class<? extends PostHook> postHook () default PostHook.class;
}
