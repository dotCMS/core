<%
   response.setContentType("text/csv");
   response.setHeader("Content-Disposition", "attachment; filename=\"report" + System.currentTimeMillis() +".csv\"");
 
 	// for testing
  // response.setContentType("text/plain");

	//print the header
	com.dotmarketing.portlets.campaigns.model.Campaign camp = (com.dotmarketing.portlets.campaigns.model.Campaign)  request.getAttribute(com.dotmarketing.util.WebKeys.CAMPAIGN_EDIT);
	java.util.List allClicks =  com.dotmarketing.portlets.campaigns.factories.ClickFactory.getClicksByParentOrderByCount(camp);
	java.util.List recipients = (java.util.List) request.getAttribute(com.dotmarketing.util.WebKeys.RECIPIENT_LIST);

	out.print("Name, eMail, Sent Date, Opened Date");
	java.util.Iterator i = allClicks.iterator();
	int x = 1;
	while(i.hasNext()){
	 	com.dotmarketing.portlets.campaigns.model.Click c = (com.dotmarketing.portlets.campaigns.model.Click) i.next();
		out.print(", Clicks on : " + c.getLink());
	}
	out.print("\n");

	java.util.Iterator iter =  recipients.iterator();

	while(iter.hasNext()){

	 	com.dotmarketing.portlets.campaigns.model.Recipient r = (com.dotmarketing.portlets.campaigns.model.Recipient) iter.next();
		out.print("\"" + com.dotmarketing.util.UtilMethods.webifyString(r.getName()) +"\",");
		out.print("\"" + com.dotmarketing.util.UtilMethods.webifyString(r.getEmail()) +"\",");
		out.print("\"" + com.dotmarketing.util.UtilMethods.webifyString(com.dotmarketing.util.UtilMethods.dateToHTMLDate(r.getSent()))+"\",");
		out.print("\"" + com.dotmarketing.util.UtilMethods.webifyString(com.dotmarketing.util.UtilMethods.dateToHTMLDate(r.getOpened()))+"\",");
		java.util.List clicks = com.dotmarketing.factories.InodeFactory.getChildrenClass(r, com.dotmarketing.portlets.campaigns.model.Click.class);
		i = clicks.iterator();
		while(i.hasNext()){
			int cCount = 0;
		 	com.dotmarketing.portlets.campaigns.model.Click c = (com.dotmarketing.portlets.campaigns.model.Click) i.next();
			java.util.Iterator i2 =  clicks.iterator();
			boolean printClick = false;
			while(i2.hasNext()){
			 	com.dotmarketing.portlets.campaigns.model.Click c2 = (com.dotmarketing.portlets.campaigns.model.Click) i2.next();
				if(c.getLink() != null && c2.getLink() != null && c.getLink().equals(c2.getLink())){
					cCount = c.getClickCount();
				}
			}
			out.print("\"" + cCount +"\",");
	
		}
		out.print("\n");
	}
%>