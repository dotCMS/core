package com.dotmarketing.portlets.checkurl.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used by every RenderService.
 * 
 * @author	Graziano Aliberti - Engineering Ingegneria Informatica
 * @date	Mar 13, 2012
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value=ElementType.TYPE)
public @interface DotRenderService {

}
