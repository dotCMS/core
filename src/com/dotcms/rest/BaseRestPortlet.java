package com.dotcms.rest;

import com.dotcms.repackage.javax.portlet.ActionRequest;
import com.dotcms.repackage.javax.portlet.ActionResponse;
import com.dotcms.repackage.javax.portlet.Portlet;
import com.dotcms.repackage.javax.portlet.PortletConfig;
import com.dotcms.repackage.javax.portlet.PortletException;
import com.dotcms.repackage.javax.portlet.RenderRequest;
import com.dotcms.repackage.javax.portlet.RenderResponse;
import com.dotcms.repackage.javax.ws.rs.GET;
import com.dotcms.repackage.javax.ws.rs.Path;
import com.dotcms.repackage.javax.ws.rs.PathParam;
import com.dotcms.repackage.javax.ws.rs.Produces;
import com.dotcms.repackage.javax.ws.rs.core.CacheControl;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.javax.ws.rs.core.Response.ResponseBuilder;
import com.dotcms.repackage.org.glassfish.jersey.servlet.internal.ThreadLocalInvoker;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cmis.proxy.DotInvocationHandler;
import com.dotmarketing.cmis.proxy.DotResponseProxy;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;
import com.liferay.portlet.RenderRequestImpl;
import com.liferay.portlet.RenderResponseImpl;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestWrapper;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

public abstract class BaseRestPortlet implements Portlet, Cloneable {

	public static final String PORTLET_ID = "PORTLET_ID";
	public static final String VIEW_JSP = "VIEW_JSP";

    @Override
	public void destroy() {

	}

	@Override
	public void init(PortletConfig config) throws PortletException {

	}

	@Override
	public void processAction(ActionRequest arg0, ActionResponse arg1) throws PortletException, IOException {

	}

	/**
	 * The render request will always be handled by the "render.jsp" located
	 * under the /WEB-INF/{classname}/ directory: the folder is based on the
	 * class name, lowercased, so this portlets jsp would be:
	 * /WEB-INF/jsp/restportlet/render.jsp
	 */
	@Override
	public void render(RenderRequest req, RenderResponse res)

	throws PortletException, IOException {
		HttpServletRequest request = ((RenderRequestImpl) req).getHttpServletRequest();
		HttpServletResponse response = ((RenderResponseImpl) res).getHttpServletResponse();

		try {
			response.getWriter().write( getJspResponse( request, response, this.getClass().getSimpleName(), "render" ) );
		} catch (ServletException e) {

			e.printStackTrace();
		}

	}

	private String getJspResponse ( HttpServletRequest request, HttpServletResponse response, String portletId, String jspName ) throws ServletException,
			IOException {

		@SuppressWarnings("rawtypes")
		InvocationHandler dotInvocationHandler = new DotInvocationHandler(new HashMap());

		DotResponseProxy responseProxy = (DotResponseProxy) Proxy.newProxyInstance(DotResponseProxy.class.getClassLoader(),
				new Class[] { DotResponseProxy.class }, dotInvocationHandler);

		jspName = (!UtilMethods.isSet(jspName)) ? "render" : jspName;

		String path = "/WEB-INF/jsp/" + portletId.toLowerCase() + "/" + jspName + ".jsp";

		HttpServletResponseWrapper responseWrapper = new ResponseWrapper( responseProxy );

		Logger.debug(this.getClass(), "trying: " + path);

		try {

			try {
				request.getRequestDispatcher( path ).include( request, responseWrapper );

			} catch ( ClassCastException e ) {//Fallback if the normal flow does not work, posible on app servers like weblogic
				Logger.debug(this.getClass(), "ClassCastException: ", e);

				//Read the request proxy sent to jersey and look for its handler to get the object behind that proxy
				ThreadLocalInvoker localInvoker = (ThreadLocalInvoker) Proxy.getInvocationHandler( request );
				Object proxiedObject = localInvoker.get();

				if ( proxiedObject instanceof ServletRequestWrapper ) {
					ServletRequestWrapper servletRequestWrapper = (ServletRequestWrapper) proxiedObject;
					request.getRequestDispatcher( path ).include( servletRequestWrapper, responseWrapper );
				} else if ( proxiedObject instanceof ServletRequest ) {
					ServletRequest servletRequest = (ServletRequest) proxiedObject;
					request.getRequestDispatcher( path ).include( servletRequest, responseWrapper );
				}
			}

			String responseString = ((ResponseWrapper) responseWrapper).getResponseString();
			return responseString;

		} catch (Exception e) {
			Logger.debug(this.getClass(), "unable to parse: " + path);
			Logger.error( this.getClass(), e.toString(), e );
			StringWriter sw = new StringWriter();
			sw.append("<div style='padding:30px;'>");
			sw.append("unable to parse: <a href='" + path + "' target='debug'>" + path + "</a>");
			sw.append("<hr>");
			sw.append("<pre style='width:90%;overflow:hidden;white-space:pre-wrap'>");
			sw.append(e.toString());

			sw.append("</pre>");
			sw.append("</div>");
			return sw.toString();

		}

	}

	@GET
	@Path("/layout/{params:.*}")
	@Produces("text/html")
	public Response getLayout ( @Context HttpServletRequest request, @Context HttpServletResponse response, @PathParam( "params" ) String params )
			throws DotDataException, DotSecurityException, ServletException, IOException, DotRuntimeException,
			PortalException, SystemException {

		User user = WebAPILocator.getUserWebAPI().getLoggedInUser(request);

		com.liferay.portal.model.Portlet portlet = null;
		String jspName = null;
		request.setAttribute(VIEW_JSP, "render");
		try {
			String[] x = params.split("/");
			portlet = APILocator.getPortletAPI().findPortlet(x[0]);
			request.setAttribute(PORTLET_ID, portlet.getPortletId());
			jspName = x[1];
			request.setAttribute(VIEW_JSP, jspName);
		} catch (ArrayIndexOutOfBoundsException aiob) {

			Logger.debug(this.getClass(), aiob.getMessage());
		} catch (Exception e) {
			com.dotmarketing.util.Logger.error(this.getClass(), e.getMessage(), e);
			ResponseBuilder builder = Response.status(500);
			return builder.build();
		}

		try {
			if (user == null
					|| !com.dotmarketing.business.APILocator.getLayoutAPI().doesUserHaveAccessToPortlet(
							portlet.getPortletId(), user)) {
				Logger.error(this.getClass(), "Invalid User  " + user + "  attempting to access this portlet");
				ResponseBuilder builder = Response.status(403);
				return builder.build();
			}
		} catch (Exception e2) {
			com.dotmarketing.util.Logger.error(this.getClass(), e2.getMessage(), e2);
			ResponseBuilder builder = Response.status(500);
			return builder.build();
		}

		ResponseBuilder builder = Response.ok( getJspResponse( request, response, portlet.getPortletId(), jspName ), "text/html" );
		CacheControl cc = new CacheControl();
		cc.setNoCache(true);

		return builder.cacheControl(cc).build();

	}

	public class ResponseWrapper extends HttpServletResponseWrapper{
		private ByteArrayOutputStream output;
		private StringWriter writer;
		private int contentLength;
		private String contentType;

		public ResponseWrapper(HttpServletResponse response) {
			super(response);
			output = new ByteArrayOutputStream();
			writer = new StringWriter();
		}

		public ServletOutputStream getOutputStream() {
			return new FilterServletOutputStream(output);
		}

		public byte[] getData() {
			return output.toByteArray();
		}

		public String getResponseString() {
			return UtilMethods.isSet(writer.toString()) ? writer.toString() : output.toString();
		}

		public PrintWriter getWriter() throws IOException {
			PrintWriter pw = new PrintWriter(writer);
			return pw;
		}

		public void setContentType(String type) {
			this.contentType = type;
			super.setContentType(type);
		}

		public String getContentType() {
			return this.contentType;
		}

		public int getContentLength() {
			return contentLength;
		}

		public void setContentLength(int length) {
			this.contentLength=length;
			super.setContentLength(length);
		}
	}

	// This class is used by the wrapper for getOutputStream() method
	// to return a ServletOutputStream object.
	public class FilterServletOutputStream extends ServletOutputStream {
		private DataOutputStream stream;

		public FilterServletOutputStream(OutputStream output) {
			stream = new DataOutputStream(output);
		}

		public void write(int b) throws IOException {
			stream.write(b);
		}

		public void write(byte[] b) throws IOException {
			stream.write(b);
		}

		public void write(byte[] b, int off, int len) throws IOException {
			stream.write(b, off, len);
		}
	}

}
