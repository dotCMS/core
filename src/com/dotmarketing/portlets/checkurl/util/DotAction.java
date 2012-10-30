package com.dotmarketing.portlets.checkurl.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used for the action method into the ActionService class.
 * 
 * @author	Graziano Aliberti - Engineering Ingegneria Informatica
 * @date	Mar 13, 2012
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value=ElementType.METHOD)
public @interface DotAction {
	
	/**
	 * The action name that must map the dot_action_method parameter into the <portlet:actionURL> tag.
	 * @return
	 */
	String action();
	
	/**
	 * The description of what the method do.
	 * @return
	 */
	String description() default "";
	
	/**
	 * The outcome is the name of which render method must be invoked. This name mus map the render property of the @DotRender method into the RenderService.
	 * @return
	 */
	String outcome();
}
