package com.dotmarketing.portlets.checkurl.util;

import java.lang.reflect.Method;
import java.util.List;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import com.dotmarketing.portal.struts.DotPortletAction;
import com.liferay.portlet.ActionRequestImpl;
import com.liferay.portlet.ActionResponseImpl;
import com.liferay.portlet.RenderRequestImpl;
import com.liferay.portlet.RenderResponseImpl;

/**
 * The main class to extend for create both RenderService and ActionService. This is also used into a struts-config.xml.<br /><br />
 * Example:<br />
 * <pre>
 * {@code
 * 		<action path="/ext/testportlet/view_test_portlet" type="com.eng.dotmarketing.framework.DispatcherService">
 * 			<forward name="portlet.ext.plugins.it.eng.dotcms.testPortlet.struts" path="portlet.ext.plugins.it.eng.dotcms.testPortlet.struts" />
 *		</action>
 * }
 * </pre>
 * <br />
 * The only two methods that it implements are <strong>_render</strong>, that process all the renderRequest and invoke the appropriate render method of the RenderService, and <strong>_processAction</strong>,
 * that process all the actionRequest and invoke the appropriate action method of the ActionService. <br /><br />
 * 
 * 
 * @author	Graziano Aliberti - Engineering Ingegneria Informatica
 * @date	Mar 13, 2012
 */
public class DispatcherService extends DotPortletAction {
		
	@Override
	public ActionForward render(ActionMapping mapping, ActionForm form, PortletConfig config,
			RenderRequest req, RenderResponse res) throws Exception {
		String portletName = config.getPortletName();
		String renderMethod = req.getParameter(RENDER_METHOD_PARAM);
		List<DispatcherService> renderServices = RenderServices.INSTANCE.getRenderServicesByPortlet(portletName);
		//the render method parameter is set
		if(null!=renderMethod){			
			if(null!=renderServices){
				for(DispatcherService renderService : renderServices){
					for(Method m : renderService.getClass().getMethods()){
						if(m.isAnnotationPresent(DotRender.class)){
							DotRender render = m.getAnnotation(DotRender.class);
							if(render.render().equals(renderMethod))
								return (ActionForward)m.invoke(renderService, mapping, form, config, req, res);
						}						
					}
				}
				return null;
			}else
				return null;
		}else{//in this case we go to the default render method, if exist 
			for(DispatcherService renderService : renderServices){
				for(Method m : renderService.getClass().getMethods()){
					DotRender render = m.getAnnotation(DotRender.class);
					if(null!=render){
						if(render.isDefaultRender())
							return (ActionForward)m.invoke(renderService, mapping, form, config, req, res);
					}
				}
			}
			return null;
		}
	}

	@Override
	public void processAction(ActionMapping mapping, ActionForm form, PortletConfig config,
			ActionRequest req, ActionResponse res) throws Exception {
		String portletName = config.getPortletName();
		String actionMethod = req.getParameter(ACTION_METHOD_PARAM);
		List<DispatcherService> actionServices = ActionServices.INSTANCE.getActionServicesByPortlet(portletName);
		if(null!=actionServices){
			for(DispatcherService actionService : actionServices){
				for(Method m : actionService.getClass().getMethods()){
					if(m.isAnnotationPresent(DotAction.class)){
						DotAction action = m.getAnnotation(DotAction.class);
						if(action.action().equals(actionMethod)){
							m.invoke(actionService, mapping, form, config, req, res);
							_setOutcome(res, action.outcome());
							return;
						}
					}
						
				}
			}
		}
	}

}
