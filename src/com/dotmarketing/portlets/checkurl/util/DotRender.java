package com.dotmarketing.portlets.checkurl.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used for the render method into the RenderService class.
 * 
 * @author	Graziano Aliberti - Engineering Ingegneria Informatica
 * @date	Mar 13, 2012
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value=ElementType.METHOD)
public @interface DotRender {
	
	/**
	 * The render name that must map the dot_render_method parameter into the <portlet:renderURL> tag.
	 * @return
	 */
	String render();
	
	/**
	 * The description of what the method do.
	 * @return
	 */
	String description() default "";
	
	/**
	 * If one method is annotated with @DotRender and has a isDefaultRender set to true, than if in the renderRequest there isn't a dot_render_method parameter this method is invoked.<br />
	 * Remember that for all the renderService that you will define for a portlet ONLY ONE method can be annotated with isDefaultRender setted to true.
	 * @return
	 */
	boolean isDefaultRender() default false; 
}
