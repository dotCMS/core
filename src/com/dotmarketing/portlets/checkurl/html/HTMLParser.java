package com.dotmarketing.portlets.checkurl.html;

import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import static com.dotmarketing.portlets.checkurl.html.util.HTMLUtil.ANCHOR;
import static com.dotmarketing.portlets.checkurl.html.util.HTMLUtil.HREF;
import static com.dotmarketing.portlets.checkurl.html.util.HTMLUtil.TITLE;
import static com.dotmarketing.portlets.checkurl.html.util.HTMLUtil.HTTP;
import static com.dotmarketing.portlets.checkurl.html.util.HTMLUtil.HTTPS;
import static com.dotmarketing.portlets.checkurl.html.util.HTMLUtil.PARAGRAPH;
import com.dotmarketing.portlets.checkurl.bean.Anchor;
import com.dotmarketing.portlets.checkurl.html.util.HTMLUtil;

public class HTMLParser {
	
	public static List<Anchor> getAnchors(StringBuilder content){
		List<Anchor> anchorList = new ArrayList<Anchor>();
		Document doc = Jsoup.parse(content.toString());
		Elements links = doc.select(ANCHOR);
		for(Element link:links){
			String href = link.attr(HREF);
			Anchor a = new Anchor();
			if(href.startsWith(HTTP) || href.startsWith(HTTPS)){ //external link				
				a.setExternalLink(HTMLUtil.getURLByString(href));
				a.setTitle(link.attr(TITLE));
				a.setInternalLink(null);
				a.setInternal(false);	
				anchorList.add(a);
			}else if(!(href.startsWith(PARAGRAPH))){ //internal link				
				a.setExternalLink(null);
				a.setTitle(link.attr(TITLE));
				a.setInternalLink(href);
				a.setInternal(true);
				anchorList.add(a);
			}
			
		}
		return anchorList;
	}

}
