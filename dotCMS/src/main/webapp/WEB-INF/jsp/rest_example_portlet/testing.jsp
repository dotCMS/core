
<%@page import="com.dotcms.rest.config.RestServiceUtil"%>
<%@page import="com.dotcms.rest.config.DotRestApplication"%>
<div class="portlet-wrapper">

	<h2>Testing JSP Example</h2>
	<hr>
	<style>
	.codeExample{
		margin:10px;
		font-family:monospace;
		color:black;

	}
	</style>
	<div style="padding:30px;">
		Again, try your <a href="javascript:window.history.back()">back button</a>.  If you got here from the main portlet screen, you should go back there.
		Calling the view using dotAjaxNav.show() has the benefit of allowing your users to use the back and forward buttons to navigate a complex webapp.
	</div>
	<div style="padding-left:30px;padding-bottom:30px">
			<a href="javascript:dotAjaxNav.show('/api/portlet/<%=request.getAttribute("PORTLET_ID") %>/');">Back to default (render) jsp</a> <br>&nbsp;<br>
	</div>
	<h2>JSON Webservices</h2>
	<hr>
	<div style="padding:30px;">
	

	
		The BaseRestPortlet can be extended to provide Jersey based webservices 
		via annotaions.  The links below link to a very simple webservice that looks like this:
	</div>
	<div style="padding:0px 30px "><pre>@Path("/restexample")
public class RestExamplePortlet extends BaseRestPortlet {

        @GET
        @Path("/test/{params:.*}")
        @Produces("application/json")
        public Response loadJson(@Context HttpServletRequest request,
                        @PathParam("params") String params) throws DotStateException,
                        DotDataException, DotSecurityException {

                CacheControl cc = new CacheControl();
                cc.setNoCache(true);

                ResponseBuilder builder = Response
                                .ok("{\"test\":\"test\"}", "application/json");
                return builder.cacheControl(cc).build();

        }

}
                        </pre>
       </div>
      <div style="padding:30px;">
		You can see that the webservice is used to return json when you click on
	
	
	

		<a href="javascript:dotAjaxNav.show('/api/restexample/test/');">get me some json</a> or in the raw <a href="/api/restexample/test/"  target="newinw"> here</a>.
		
	</div>

</div>