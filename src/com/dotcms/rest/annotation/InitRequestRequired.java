package com.dotcms.rest.annotation;


import com.dotcms.repackage.javax.ws.rs.NameBinding;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * When your resource method needs a initial attributes on the request, you can annotated it with this one.
 *
 * The following stuff will be init:
 * <ul>
 *     <li>The Context Path {@link com.liferay.portal.util.WebKeys}.CTX_PATH</li>
 *     <li>The Context Path {@link com.liferay.portal.util.PropsUtil}.CTX</li>
 * </ul>
 *
 * @author jsanca
 */
@NameBinding
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(value = RetentionPolicy.RUNTIME)
public @interface InitRequestRequired {

} // E:O:F:AccessControlAllowOrigin
