package com.dotmarketing.portlets.checkurl.util;

import java.io.IOException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.methods.GetMethod;

import com.dotmarketing.portlets.checkurl.bean.Anchor;
import com.dotmarketing.portlets.checkurl.bean.CheckURLBean;
import com.dotmarketing.portlets.checkurl.html.HTMLParser;


/**
 * Main class that 
 * 
 * @author	Graziano Aliberti
 * @date	24/02/2012 	
 *
 */
public class LinkChecker {
	
	public static List<CheckURLBean> checkURL(String content) {
		return checkURL(new StringBuilder(content));
	}

	public static List<CheckURLBean> checkURL(StringBuffer content) {
		return checkURL(new StringBuilder(content));
	}	
	
	public static List<CheckURLBean> checkURL(StringBuilder content) {
		//retrieve the anchor list by content
		List<Anchor> anchorList = HTMLParser.getAnchors(content);
		List<CheckURLBean> result = new ArrayList<CheckURLBean>();
		for(Anchor a:anchorList){
			if(null!=a.getExternalLink() && (!a.isInternal())){ //external link
				HttpClient client = new HttpClient();
				loadProxy(client);
				HttpMethod method = new GetMethod(a.getExternalLink().absoluteURL());
				if(a.getExternalLink().isWithParameter())
					method.setQueryString(a.getExternalLink().getQueryString());
				int statusCode = 0;
				try{
					statusCode = client.executeMethod(method);
					if(statusCode!=200){
						CheckURLBean c = new CheckURLBean();
						c.setUrl(a.getExternalLink().absoluteURL());
						c.setStatusCode(statusCode);
						c.setTitle(a.getTitle());
						c.setInternalLink(false);
						result.add(c);
					}
				} catch(ConnectException e){
					CheckURLBean c = new CheckURLBean();
					c.setUrl(a.getExternalLink().absoluteURL());
					c.setStatusCode(-1);
					c.setTitle(a.getTitle());
					c.setInternalLink(false);
					result.add(c);					
				} catch (HttpException e) {
					CheckURLBean c = new CheckURLBean();
					c.setUrl(a.getExternalLink().absoluteURL());
					c.setStatusCode(-1);
					c.setTitle(a.getTitle());
					c.setInternalLink(false);
					result.add(c);					
				} catch (IOException e) {
					CheckURLBean c = new CheckURLBean();
					c.setUrl(a.getExternalLink().absoluteURL());
					c.setStatusCode(-1);
					c.setTitle(a.getTitle());
					c.setInternalLink(false);
					result.add(c);					
				}
			}else{ //internal link...to be checked into the dotCMS context
				CheckURLBean c = new CheckURLBean();
				c.setUrl(a.getInternalLink());
				c.setTitle(a.getTitle());
				c.setInternalLink(true);
				result.add(c);
			}
		}
		return result;
	}
	
	@SuppressWarnings("deprecation")
	private static void loadProxy(HttpClient client){
		if(ProxyManager.INSTANCE.isLoaded()){
			if(ProxyManager.INSTANCE.getConnection().isProxy()){
				client.getHostConfiguration().setProxy(ProxyManager.INSTANCE.getConnection().getProxyHost(), ProxyManager.INSTANCE.getConnection().getProxyPort());
				if(ProxyManager.INSTANCE.getConnection().isProxyRequiredAuth()){
					HttpState state = new HttpState();
					state.setProxyCredentials(null, null,
					   new UsernamePasswordCredentials(ProxyManager.INSTANCE.getConnection().getProxyUsername(), ProxyManager.INSTANCE.getConnection().getProxyPassword()));
					client.setState(state);
				}
			}
		}		
	}	

}
