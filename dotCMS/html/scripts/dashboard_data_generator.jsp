 <%@page import="com.dotmarketing.portlets.contentlet.model.Contentlet"%>
<%@page import="com.dotmarketing.beans.Clickstream404"%>
<%@page import="com.dotmarketing.factories.ClickstreamRequestFactory"%>
<%@page import="com.dotmarketing.cms.factories.PublicCompanyFactory"%>
<%@page import="com.dotmarketing.beans.ClickstreamRequest"%>
<%@page import="com.dotmarketing.beans.Host"%>
<%@page import="java.util.ArrayList"%>
<%@page import="com.dotmarketing.beans.Identifier"%>
<%@page import="com.dotmarketing.util.UUIDGenerator"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.beans.Clickstream"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Random"%>
<%@page import="com.dotmarketing.common.db.DotConnect"%>
<%@page import="com.dotmarketing.factories.ClickstreamFactory"%>
<%@page import="java.io.InputStreamReader"%>
<%@page import="java.net.ProtocolException"%>
<%@page import="java.net.MalformedURLException"%>
<%@page import="java.net.URL"%>
<%@page import="java.io.BufferedReader"%>
<%@page import="java.io.OutputStreamWriter"%>
<%@page import="java.net.HttpURLConnection"%>
<%@page import="com.dotcms.enterprise.DashboardProxy" %>
<%@page import="com.dotmarketing.portlets.dashboard.business.DashboardDataGenerator" %>
<%@page import="java.util.Calendar" %>
<%@page import="java.util.Date" %>
<%

String action = (request.getParameter("action") !=null) 
				?  request.getParameter("action")
						: "add";
int visits = 500;
int days = 30;
String[] urls = {"/home/index.html","/products/index.html","/about-us/index.html"};



if("add".equals(action)){
	List<Contentlet> cons = APILocator.getContentletAPI().search("*", 100, 0, "modDate", APILocator.getUserAPI().getSystemUser(), false);

	for(int g = 0;g<visits;g++){
	
	

		Host host = APILocator.getHostAPI().findDefaultHost(APILocator.getUserAPI().getSystemUser(), false);
	
		
		List<Identifier> ids = new ArrayList<Identifier>();
		
		for(String x : urls){
			Identifier id = APILocator.getIdentifierAPI().find(host, x);
			ids.add( id );
		}

	
		
		String dmid = UUIDGenerator.generateUuid();
	    // one month back
	    int secMax = 60*60*24*days;
	
	    Random generator = new Random( );
	    int secBack =   generator.nextInt(secMax);
	 	
	
	    
	    Calendar cal = Calendar.getInstance();
	    cal.add(Calendar.SECOND, -secBack);
	
	   
	    
		
		Clickstream click = new Clickstream();
		click.setBot(false);
		click.setStart(cal.getTime());
	    cal.add(Calendar.SECOND, +300);
		click.setLastRequest(cal.getTime());
		cal.add(Calendar.SECOND, -300);
		click.setUserAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_6_8) AppleWebKit/537.17 (KHTML, like Gecko) Chrome/24.0.1312.52 Safari/537.17");
		click.setHostId(host.getIdentifier());
		click.setRemoteAddress("127.0.0.1");
		click.setCookieId(dmid);
		click.setNumberOfRequests(urls.length);
		click.setInitialReferrer("http://google.com/search?q=java-cms");
		click.setFirstPageId(ids.get(0).getId());
		click.setLastPageId(ids.get(ids.size()-1).getId());

	    ClickstreamFactory.save(click);
	    
	    
	    DotConnect dc = new DotConnect();
	    String select_clickstream_id = "select clickstream_id from clickstream where cookie_id = ?";
	    
	    dc.setSQL(select_clickstream_id);
	    dc.addParam(dmid);
	
	
	    
	    int click_id = dc.getInt("clickstream_id");
	    
	    
		int ord = 0;
	    for(Identifier x : ids){
	    	ClickstreamRequest creq = new ClickstreamRequest();
	    	creq.setClickstreamId(click_id);
	    	creq.setLanguageId(APILocator.getLanguageAPI().getDefaultLanguage().getId());
			creq.setHostId(host.getIdentifier());
	    	creq.setRequestOrder(ord++);
	    	creq.setServerName(host.getHostname());
	    	creq.setTimestamp(cal.getTime());
	    	cal.add(Calendar.SECOND, new Random().nextInt(600));
	    	creq.setProtocol("http");
			creq.setRequestURI(x.getPath());
			creq.setAssociatedIdentifier(x.getId());
	    	ClickstreamRequestFactory.save(creq);
	
	    }
	    
	    Clickstream404 c404 = new Clickstream404();
	    c404.setHostId(host.getIdentifier());
	    c404.setTimestamp(cal.getTime());
	    c404.setRefererURI("http://nowhere.com/testing.html");
	    c404.setRequestURI("/this/doesnotwork/" + secBack+ ".html");
	
	    ClickstreamFactory.save404(c404);

	}

	out.println("added " + visits );



}


if("run".equals(action)){

	out.println("Starting dashboard data grind");
	out.flush();
	APILocator.getDashboardAPI().populateAnalyticSummaryTables();
}



if("del".equals(action)){
    DotConnect dc = new DotConnect();
  	dc.setSQL("delete from clickstream_request");
  	dc.getResult();
  	dc.setSQL("delete from clickstream");
  	dc.getResult();
  	dc.setSQL("delete from clickstream_404");
  	dc.getResult();
  	
  	
  	
  	dc.setSQL("delete from dashboard_user_preferences");
  	dc.getResult();
  	dc.setSQL("delete from analytic_summary_404");
  	dc.getResult();
	dc.setSQL("delete from analytic_summary_referer");
  	dc.getResult();
	dc.setSQL("delete from analytic_summary_workstream");
  	dc.getResult();
	dc.setSQL("delete from analytic_summary_visits");
	dc.getResult();
	dc.setSQL("delete from analytic_summary_content");
  	dc.getResult();
	dc.setSQL("delete from analytic_summary_pages");
  	dc.getResult();
	dc.setSQL("delete from analytic_summary");
  	dc.getResult();
  	
	dc.setSQL("delete from analytic_summary_period");
  	dc.getResult();



  	out.println("data deleted");
}

%>
<html>
<body>

<ul>

<li><a href="?action=add">add clickstream data</a>
<li><a href="?action=run">Run Dashboard Job</a>
<li><a href="?action=del">delete data</a>



</body>

</html> 