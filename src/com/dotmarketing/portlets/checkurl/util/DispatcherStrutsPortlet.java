package com.dotmarketing.portlets.checkurl.util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.portlet.PortletConfig;
import javax.portlet.PortletException;

import com.dotmarketing.util.Logger;
import com.liferay.portlet.StrutsPortlet;

/**
 * Extension of StrutsPortlet that dispatch the action and the render request across a list of configurable services.
 * <br /><br />
 * At startup load the two lists of actionService and renderService configured into the init-param of the portlet.xml.
 * <br />
 * Example:<br /><br />
 * <pre>
 * {@code
 * <portlet>
 *	<portlet-name>EXT_TEST_PORTLET</portlet-name>
 *	<display-name>Broken Links</display-name>
 *	<portlet-class>com.eng.dotmarketing.framework.portlets.DispatcherStrutsPortlet</portlet-class>
 *	<init-param>
 *		<name>view-action</name>
 *		<value>/ext/testportlet/view_test_portlet</value>
 *	</init-param>
 *	<init-param>
 *		<name>action-services</name>
 *		<value>it.eng.dotcms.test.business.portlets.actionservices.TestActionService</value>		
 *	</init-param>		
 *	<init-param>
 *		<name>render-services</name>
 *		<value>it.eng.dotcms.test.business.portlets.renderservices.TestRenderService</value>		
 *	</init-param>	
 *	<expiration-cache>0</expiration-cache>
 *	<supports>
 *		<mime-type>text/html</mime-type>
 *	</supports>
 *	<resource-bundle>com.liferay.portlet.StrutsResourceBundle</resource-bundle>
 *	<security-role-ref>
 *		<role-name>CMS Administrator</role-name>
 *	</security-role-ref>
 *	<security-role-ref>
 *		<role-name>CMS User</role-name>
 *	</security-role-ref>	
 * </portlet>
 * }
 * </pre>
 * <br />
 * The most important difference between a standard StrutsPortlet configuration is the declaration of two new init-param: <strong>action-service</strong> and <strong>render-service</strong>.<br />
 * <h3>action-service</h3><br />
 * This init-param accept a comma separated list of ActionService class that contains all the action method that can be used into this portlet.<br /><br /> 
 * <h3>render-service</h3><br />
 * This init-param accept a comma separated list of RenderService class that contains all the render method that can be used into this portlet.<br /><br />
 *   
 * @author	Graziano Aliberti - Engineering Ingegneria Informatica
 * @date	Mar 9, 2012
 */
public class DispatcherStrutsPortlet extends StrutsPortlet {

	private List<DispatcherService> actionServices;
	private List<DispatcherService> renderServices;

	/**
	 * Load the two classes list for the action-service and the render-service.<br />
	 * According with the ActionService and RenderService roles, this method do the following steps:<br /><br />
	 * <strong>action-services</strong>
	 * <ul>
	 * 	<li>For every class check if it's annotated with the @DotActionService annotation: if not than a new PortletException is throwed;</li>
	 * </ul>
	 * <br /><br />
	 * <strong>render-services</strong>
	 * <ul>
	 * 	<li>For every class check if it's annotated with the @DotRenderService annotation: if not than a new PortletException is throwed;</li>
	 * 	<li>Count the number of defaultRender method created: if this number is great than 1 or equals 0 than a new PortletException is throwed</li>
	 * </ul>
	 */
	public void init(PortletConfig config) throws PortletException {		
		super.init(config);
		actionServices = new ArrayList<DispatcherService>();
		renderServices = new ArrayList<DispatcherService>();
		loadActionServices();
		loadRenderServices();
		//store the services for the portlet...		
		Logger.info(DispatcherStrutsPortlet.class, "Initialize dispatcher portlet...store the action services");
		ActionServices.INSTANCE.addActionServicesForPortlet(actionServices, getPortletName());
		Logger.info(DispatcherStrutsPortlet.class, "Initialize dispatcher portlet...store the render services");
		RenderServices.INSTANCE.addRenderServicesForPortlet(renderServices, getPortletName());
	}	
	
	private void loadActionServices() throws PortletException{
		if(null!=getInitParameter("action-services")){
			String[] actionServicesArr = getInitParameter("action-services").split("[,]");
			Logger.info(DispatcherStrutsPortlet.class, "Initialize dispatcher portlet...get the action services");
			for(String singleClass : actionServicesArr){
				try {
					Class<?> clazz = Class.forName(singleClass);
					if(!clazz.isAnnotationPresent(DotActionService.class))
						throw new PortletException("The actionService class ("+clazz.getName()+") is not a valid ActionService: is this class annotated with @DotActionService annotation?");
					Object obj = clazz.newInstance();
					if(obj instanceof DispatcherService){
						DispatcherService action = (DispatcherService)obj;					
						actionServices.add(action);
					}
				} catch (InstantiationException e) {
					Logger.error(DispatcherStrutsPortlet.class, "Error in load action service",e);
					throw new PortletException(e);
				} catch (IllegalAccessException e) {
					Logger.error(DispatcherStrutsPortlet.class, "Error in load action service",e);
					throw new PortletException(e);
				} catch (ClassNotFoundException e) {
					Logger.error(DispatcherStrutsPortlet.class, "Error in load action service",e);
					throw new PortletException(e);
				}
			}			
		}else
			Logger.warn(DispatcherStrutsPortlet.class, getPortletName()+": The portlet definition don't have action-services.");
	}
	
	private void loadRenderServices() throws PortletException{
		if(null!=getInitParameter("render-services")){
			String[] renderServicesArr = getInitParameter("render-services").split("[,]");
			Logger.info(DispatcherStrutsPortlet.class, "Initialize dispatcher portlet...get the render services");
			int defaultCounter = 0;
			int globalDefaultCounter = 0;
			for(String singleClass : renderServicesArr){
				try {
					Class<?> clazz = Class.forName(singleClass);
					if(!clazz.isAnnotationPresent(DotRenderService.class))
						throw new PortletException("The renderService class ("+clazz.getName()+") is not a valid RenderService: is this class annotated with @DotRenderService annotation?");
					for(Method m : clazz.getMethods()){
						if(m.isAnnotationPresent(DotRender.class)){
							DotRender render = m.getAnnotation(DotRender.class);
							if(render.isDefaultRender()){
								defaultCounter++;
								globalDefaultCounter++;
							}
						}
					}
					if(defaultCounter>1)
						throw new PortletException("The renderService class ("+clazz.getName()+") has more than one default @DotRender method: change the code because only one default render method is allowed.");
					if(defaultCounter==0)
						throw new PortletException("In your renderService class ("+clazz.getName()+") there isn't a default @DotRender method. This maybe can create a problem with the renderURL invocation without the dot_render_method parameter.");
					Object obj = clazz.newInstance();
					if(obj instanceof DispatcherService){
						DispatcherService action = (DispatcherService)obj;					
						renderServices.add(action);
					}
				} catch (InstantiationException e) {
					Logger.error(DispatcherStrutsPortlet.class, "Error in load render service",e);
					throw new PortletException(e);
				} catch (IllegalAccessException e) {
					Logger.error(DispatcherStrutsPortlet.class, "Error in load render service",e);
					throw new PortletException(e);
				} catch (ClassNotFoundException e) {
					Logger.error(DispatcherStrutsPortlet.class, "Error in load render service",e);
					throw new PortletException(e);
				}
			}
			if(globalDefaultCounter>1)
				throw new PortletException("In your renderServices there are more than one default @DotRender method. Change your code because only one default render method is allowed.");
			if(globalDefaultCounter==0)
				throw new PortletException("In your renderServices there isn't a default @DotRender method. This maybe can create a problem with the renderURL invocation without the dot_render_method parameter.");
		}else
			Logger.warn(DispatcherStrutsPortlet.class, getPortletName()+": The portlet definition don't have render-services.");
	}

}
