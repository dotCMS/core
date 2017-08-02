package com.dotcms.api.di;

import java.lang.annotation.*;

/**
 *  A {@link com.dotcms.api.aop.MethodInterceptor} annotated with  DotBean annotation
 *  will be added to a guice ioc/aop container.
 *
 *  @author jsanca
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface DotBean {


}