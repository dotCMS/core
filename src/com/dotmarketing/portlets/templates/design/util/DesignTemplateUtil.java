package com.dotmarketing.portlets.templates.design.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * This class contains a list of utility's methods for the design of the template 
 * 
 * @author	Graziano Aliberti - Engineering Ingegneria Informatica
 * @date	Apr 19, 2012
 */
public class DesignTemplateUtil {
	
	private static String PATH_CSS_YUI = "/html/css/template/reset-fonts-grids.css";
	
	/**
	 * Returns the body of the drawed template, including all the main HTML tags
	 * 
	 * @param _body - the body became by jsp TemplateForm
	 * @return end body with all HTML tags
	 */
	public static StringBuffer getBody(String _body){
		StringBuffer endBody;
		Document templateBody = Jsoup.parse(_body);
		
		//adding default css for YUI Grid
		Element head = templateBody.head();
		head.append("<link rel=\"stylesheet\" type=\"text/css\" href=\""+PATH_CSS_YUI+"\">");
		
		// remove the div for file
		removeFileIconDiv(templateBody);
		
		// remove the "add container" links
		removeAddContainer(templateBody);
		
		// remove the mock containers
		removeMockContainers(templateBody);
		
		// remove the <h1> contents
		removeYuiGridContent(templateBody);
		
		// add all the js and css files 
		addJsCssFiles(templateBody);
		
		// gets the parseContainer
		getParseContainer(templateBody);
		
		endBody = new StringBuffer(templateBody.toString());
		return endBody;
	}
	
	private static void removeAddContainer(Document templateBody){
		Elements addContainers = templateBody.getElementsByClass("addContainerSpan");
		for(Element singleDiv : addContainers){
			singleDiv.remove();
		}
	}

	private static void removeMockContainers(Document templateBody){
		Elements mockContainers = templateBody.getElementsByClass("titleContainerSpan");
		for(Element singleDiv : mockContainers){
			singleDiv.remove();
		}
	}
	
	private static void removeYuiGridContent(Document templateBody){
		Elements h1 = templateBody.getElementsByTag("h1");
		for(Element singleH1 : h1){
			singleH1.remove();
		}
	}
	
	private static void removeFileIconDiv(Document templateBody){
		Element divFilesIcons = templateBody.getElementById("fileContainerDiv");
		divFilesIcons.remove();
	}
	
	private static void getParseContainer(Document templateBody){
		Elements divHiddenParseContainer = templateBody.getElementsByAttributeValue("style", "display: none;");
		for(Element singleDiv : divHiddenParseContainer){
			String html = singleDiv.html();
			Element parent = singleDiv.parent();
			singleDiv.remove();
			parent.append(html);
		}
	}
	
	private static void addJsCssFiles(Document templateBody){
		Element head = templateBody.head();
		Element divFilesToAdd = templateBody.getElementById("jsCssToAdd");
		Elements filesToAdd = divFilesToAdd.getElementsByAttributeValueStarting("id", "div_");
		for(Element singleFile : filesToAdd){
			head.append(singleFile.html());
			singleFile.remove();
		}
		divFilesToAdd.remove();
	}
	
}
