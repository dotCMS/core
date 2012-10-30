package com.dotmarketing.portlets.checkurl.util;

import com.dotmarketing.portlets.checkurl.bean.CheckURLBean;

import java.util.List;

/**
 * Utility class for replace placeholders into mail msg before it was send.
 * 
 * @author	Graziano Aliberti - Engineering Ingegneria Informatica
 * @date	Mar 2, 2012
 */
public class FormatUtil {
	
	private static String PLACEHOLDER_LIST_LINKS = "{2}";
	private static String PLACEHOLDER_FULL_NAME = "{0}";
	private static String PLACEHOLDER_CONTENT_TITLE = "{1}";
	
	public static String buildEmailBodyWithLinksList(String emailBody, String userFullName, String contentTitle, List<CheckURLBean> list){
		StringBuilder sb = new StringBuilder();
		sb.append("<ul>");
		for(CheckURLBean c : list){
			sb.append("<li>");
			sb.append("<strong> Link: </strong>");
			sb.append(c.getUrl());
			sb.append("; <strong> Http return code: </strong>");
			sb.append(c.getStatusCode());
			sb.append("</li>");
		}
		sb.append("</ul>");
		return emailBody.replace(PLACEHOLDER_LIST_LINKS, sb.toString()).replace(PLACEHOLDER_FULL_NAME, userFullName).replace(PLACEHOLDER_CONTENT_TITLE, contentTitle);
	}
	
	public static String buildPopupMsgWithLinksList(String popupMsg, List<CheckURLBean> list){
		StringBuilder sb = new StringBuilder();
		sb.append("<br />");
		for(CheckURLBean c : list){
			sb.append("<br />");
			sb.append("&nbsp-&nbsp");
			sb.append("<strong> Link: </strong>");
			sb.append(c.getUrl());
			sb.append("; <strong> Http code: </strong>");
			sb.append(c.getStatusCode());
			sb.append("; <strong> Title: </strong>");
			sb.append(c.getTitle());
		}
		return popupMsg.replace(PLACEHOLDER_LIST_LINKS, sb.toString());
	}
	
}
