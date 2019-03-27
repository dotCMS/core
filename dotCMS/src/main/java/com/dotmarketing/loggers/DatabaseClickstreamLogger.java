package com.dotmarketing.loggers;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.dotmarketing.beans.Clickstream;
import com.dotmarketing.beans.Clickstream404;
import com.dotmarketing.beans.ClickstreamRequest;
import com.dotmarketing.factories.ClickstreamFactory;
import com.dotmarketing.factories.ClickstreamRequestFactory;
import com.dotmarketing.util.DNSUtil;
import com.dotmarketing.util.UtilMethods;

/**
 * A simple ClickstreamLogger that outputs the entire clickstream to the <a
 * href="http://jakarta.apache.org/commons/logging.html">Jakarta Commons Logging
 * component</a>.
 * 
 * @author <a href="plightbo@hotmail.com">Patrick Lightbody</a>
 */
public class DatabaseClickstreamLogger implements ClickstreamLogger {

	public void log(Clickstream clickstream) {
		if (clickstream == null)
			return;

		
		// try to build a reverse lookup
		if(!UtilMethods.isSet(clickstream.getRemoteHostname()) && UtilMethods.isSet(clickstream.getRemoteAddress())){
			String x = clickstream.getRemoteAddress();
			try{
				x = DNSUtil.reverseDns(clickstream.getRemoteAddress());
			}
			catch(Exception e){
				
			}
			if(!clickstream.getRemoteAddress().equals(x)){
				clickstream.setRemoteHostname(x);
			}
		}
		
		
		
		
		/*
		 * Save current clickstream
		 */
		clickstream.setLastSaved(new Date());
		ClickstreamFactory.save(clickstream);
		
		List<ClickstreamRequest> myStream = new ArrayList<ClickstreamRequest>();
		myStream.addAll(clickstream.getClickstreamRequests());
		long clickStreamId = clickstream.getClickstreamId();
		if (myStream != null ) {
			for(ClickstreamRequest myClickstreamRequest  : myStream){
				myClickstreamRequest.setClickstreamId(clickStreamId);
				ClickstreamRequestFactory.save(myClickstreamRequest);
			}
		}
		
		List<Clickstream404> my404Stream = new ArrayList<Clickstream404>();
		my404Stream.addAll(clickstream.getClickstream404s());
		if (my404Stream != null ) {
			for(Clickstream404 myClickstream404  : my404Stream){
				ClickstreamFactory.save404(myClickstream404);
			}
		}

		/*
		 * Loop over old clickstreams (by long lived cookie) that don't have any user
		 * information and update them to our known information
		 * This should log people even if they don't login
		 */
		String _dotCMSID = clickstream.getCookieId();
		String _loggedInUser = clickstream.getUserId();

		if (_dotCMSID != null) {
			List<Clickstream> csl = ClickstreamFactory.getClickstreamsByCookieId(_dotCMSID);
			if (_loggedInUser == null) {
				for (Clickstream cs : csl) {
					if (cs.getUserId() != null) {
						_loggedInUser = cs.getUserId();
						break;
					}
				}
			}
			if (_loggedInUser != null) {
				for (Clickstream cs : csl) {
					if (cs.getUserId() == null) {
						cs.setUserId(_loggedInUser);
						ClickstreamFactory.save(cs);
					}
				}
			}
		}
	}
}
