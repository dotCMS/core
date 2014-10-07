
<div class="portlet-wrapper">

	<h2>REST Portlet Example (or One Pager Web Apps in dotCMS)</h2>
	<hr>
	<style>
	.codeExample{
		margin:10px;
		font-family:monospace;
		color:black;

	}
	</style>
	
	<div style="padding:30px;padding-top:10px;">
	<p>There are a number of interesting things about this REST example Portlet.</p>
	<p>First, you didn't need a page refresh to get here.  Any portlets that extend  <i>com.dotcms.rest.BaseRestPortlet</i> will automatically load the contents of the view directly into the dotCMS chrome using an Ajax call. </p>

	<p>
	Second, developing these portlets are easy.  At its simplest, you only need to provide a jsp as a view. 
	The benefits of calling or including the jsps this way is that the BaseRestPortlet also takes care of permissions and portlet authentication for these jsp based views.  This means you do not need to include boilerplate authcode at the the top of all your jsp based snippets.
	The <i>BaseRestPortlet</i> portlet views are wired by convention, the default being 
	<div class="codeExample">WEB-INF/$PORTLET_ID/render.jsp
	</div>

	You can call other jsps, either to replace the whole view or as snippets that can be included dynamically via xhr. 
	These other jsp "templates" can be gotten to by the following call:   
	<div class="codeExample">
	/api/portlet/$PORTLET_ID/$jspname
	</div> 
	minus the jsp extension.  So to render the contents of the testing.jsp in the app chrome,
	you would call (click)
	<div class="codeExample">
	<a href="javascript:dotAjaxNav.show('/api/portlet/<%=request.getAttribute("PORTLET_ID") %>/testing');">dotAjaxNav.show('/api/portlet/<%=request.getAttribute("PORTLET_ID") %>/testing')</a>
</div>
or the raw url for xhr includes.
	<div class="codeExample">
<a href="/api/portlet/REST_EXAMPLE_PORTLET/testing" >/api/portlet/REST_EXAMPLE_PORTLET/testing</a>
	</div>
	which will render the jsp under WEB-INF/REST_EXAMPLE_PORTLET/testing.jsp.
	</p>
	<p>
	To render the testing jsp inline and replace the whole screen, you can call with javascript:

	<div class="codeExample">
	dotAjaxNav.show('/api/portlet/<%=request.getAttribute("PORTLET_ID") %>/testing')
	</div>
	Calling the view using <b>dotAjaxNav.show()</b> has the benefit of allowing your users to use the <b>back and forward buttons</b> to navigate a complex webapp.



	</p>

	</div>
	<h2>JSON Webservices</h2>
	<hr>
	<div style="padding:30px;">
	

	
		Additionally, the BaseRestPortlet can be extended to provide Jersey based webservices 
		via annotaions. The links below link to a very simple webservice that looks like this:
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