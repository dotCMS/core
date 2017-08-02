package com.dotcms.api.aop.guice;

import java.lang.annotation.*;

/**
 *  A {@link com.dotcms.api.aop.MethodInterceptor} annotated with  DotBean annotation
 *  will be added to a guice ioc/aop container.
 *
 *  @author jsanca
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface MethodInterceptorConfig {

    public static final String ANY = "any";

    /**
     * The package to apply interceptor.
     * @return String
     */
    String packageMatcher() default ANY;

    /**
     * Annotation to apply the Aspect
     * @return Class
     */
    Class<? extends Annotation> annotation();
}