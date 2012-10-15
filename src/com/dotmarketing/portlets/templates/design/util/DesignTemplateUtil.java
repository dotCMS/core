package com.dotmarketing.portlets.templates.design.util;

import static com.dotmarketing.portlets.templates.design.util.DesignTemplateHtmlCssConstants.ADD_CONTAINER_SPAN_CLASS;
import static com.dotmarketing.portlets.templates.design.util.DesignTemplateHtmlCssConstants.FILE_CONTAINER_DIV_ID;
import static com.dotmarketing.portlets.templates.design.util.DesignTemplateHtmlCssConstants.FILE_TO_ADD_START_ID;
import static com.dotmarketing.portlets.templates.design.util.DesignTemplateHtmlCssConstants.FILES_TO_ADD_DIV_ID;
import static com.dotmarketing.portlets.templates.design.util.DesignTemplateHtmlCssConstants.H1_TAG;
import static com.dotmarketing.portlets.templates.design.util.DesignTemplateHtmlCssConstants.DIV_TAG;
import static com.dotmarketing.portlets.templates.design.util.DesignTemplateHtmlCssConstants.ID_ATTRIBUTE;
import static com.dotmarketing.portlets.templates.design.util.DesignTemplateHtmlCssConstants.STYLE_ATTRIBUTE;
import static com.dotmarketing.portlets.templates.design.util.DesignTemplateHtmlCssConstants.STYLE_DISPLAY_NONE;
import static com.dotmarketing.portlets.templates.design.util.DesignTemplateHtmlCssConstants.TITLE_CONTAINER_SPAN_CLASS;
import static com.dotmarketing.portlets.templates.design.util.DesignTemplateHtmlCssConstants.START_COMMENT;
import static com.dotmarketing.portlets.templates.design.util.DesignTemplateHtmlCssConstants.END_COMMENT;
import static com.dotmarketing.portlets.templates.design.util.DesignTemplateHtmlCssConstants.PATH_CSS_YUI;
import static com.dotmarketing.portlets.templates.design.util.DesignTemplateHtmlCssConstants.NAME_ATTRIBUTE;
import static com.dotmarketing.portlets.templates.design.util.DesignTemplateHtmlCssConstants.MAIN_DIV_NAME_VALUE;
import static com.dotmarketing.portlets.templates.design.util.DesignTemplateHtmlCssConstants.HEADER_ID;
import static com.dotmarketing.portlets.templates.design.util.DesignTemplateHtmlCssConstants.FOOTER_ID;
import static com.dotmarketing.portlets.templates.design.util.DesignTemplateHtmlCssConstants.SIDEBAR_ID;
import static com.dotmarketing.portlets.templates.design.util.DesignTemplateHtmlCssConstants.CLASS_ATTRIBUTE;
import static com.dotmarketing.portlets.templates.design.util.DesignTemplateHtmlCssConstants.NO_SIDEBAR_VALUE;
import static com.dotmarketing.portlets.templates.design.util.DesignTemplateHtmlCssConstants.SPLIT_BODY_ID_PREFIX;

import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.design.bean.PreviewFileAsset;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayoutRow;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.UtilMethods;

/**
 * This class contains a list of utility's methods for the design of the template.
 *
 * @author	Graziano Aliberti - Engineering Ingegneria Informatica
 * @date	Apr 19, 2012
 */
public class DesignTemplateUtil {


	/**
	 * Returns the body of the drawed template, including all the main HTML tags for the preview functionality
	 *
	 * @param _body - the body became by jsp TemplateForm
	 * @return endBody with all HTML tags
	 */
	public static StringBuffer getPreviewBody(String _body, List<PreviewFileAsset> savedFiles, String themePath, boolean header, boolean footer){
		Document templateBody = Jsoup.parse(_body);

		// adding default css for YUI Grid
		if(UtilMethods.isSet(themePath)) {
			addHeadCode(templateBody, "#dotParse('"+themePath+Template.THEME_HTML_HEAD+"')");
		}
		addHeadCode(templateBody, "<link rel=\"stylesheet\" type=\"text/css\" href=\""+PATH_CSS_YUI+"\">");

		if(UtilMethods.isSet(themePath) && header) {
			addHeaderCode(templateBody, "#dotParse('"+themePath+Template.THEME_HEADER+"')");
		}

		if(UtilMethods.isSet(themePath) && footer) {
			addFooterCode(templateBody, "#dotParse('"+themePath+Template.THEME_FOOTER+"')");
		}

		// remove the div for file
		removeFileIconDiv(templateBody);

		// remove the "add container" links
		removeAddContainer(templateBody);

		// remove the mock containers
		removeMockContainers(templateBody);

		// remove the <h1> contents
		removeYuiGridContent(templateBody);

		// add all the js and css files
//		addPreviewJsCssFiles(templateBody,savedFiles);
		addJsCssFiles(templateBody);

		// gets the parseContainer
		getParseContainer(templateBody);


		return new StringBuffer(templateBody.toString());

	}

	/**
	 * Returns the body of the drawed template, including all the main HTML tags
	 *
	 * @param _body - the body became by jsp TemplateForm
	 * @return endBody with all HTML tags
	 */
	public static StringBuffer getBody(String _body, String headCode, String themePath, boolean header, boolean footer){
		Document templateBody = Jsoup.parse(_body);

		// adding default css for YUI Grid

		if(UtilMethods.isSet(themePath)) {
			addHeadCode(templateBody, "#dotParse('"+themePath+Template.THEME_HTML_HEAD+"')");
		}
		addHeadCode(templateBody, "<link rel=\"stylesheet\" type=\"text/css\" href=\""+PATH_CSS_YUI+"\">");

		if(UtilMethods.isSet(themePath) && header) {
			addHeaderCode(templateBody, "#dotParse('"+themePath+Template.THEME_HEADER+"')");
		}

		if(UtilMethods.isSet(themePath) && footer) {
			addFooterCode(templateBody, "#dotParse('"+themePath+Template.THEME_FOOTER+"')");
		}

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

		// gets the metatag containers
		getMetatagContainers(templateBody);

		// add head code to body
		if(null!=headCode && !"".equals(headCode.trim()))
			addHeadCode(templateBody, headCode);

		return new StringBuffer(templateBody.toString());
	}

	/**
	 * Get the values for the design fields.
	 *
	 * @param drawedBody
	 * @return
	 */
	public static TemplateLayout getDesignParameters(String drawedBody){
		Document templateDrawedBody = Jsoup.parse(drawedBody);
		TemplateLayout parameters = new TemplateLayout();
		parameters.setPageWidth(getPageWithValue(templateDrawedBody));
		parameters.setHeader(hasHeader(templateDrawedBody));
		parameters.setFooter(hasFooter(templateDrawedBody));
		parameters.setLayout(getLayout(templateDrawedBody));
		parameters.setBodyRows(getSelectForBody(templateDrawedBody));
		return parameters;
	}

	/**
	 * Get the imported files inodes
	 *
	 * May 7, 2012 - 5:31:05 PM
	 */
	public static List<PreviewFileAsset> getFilesInodes(String _body){
		Document templateBody = Jsoup.parse(_body);
		List<PreviewFileAsset> result = new ArrayList<PreviewFileAsset>();
		Element divFilesToAdd = templateBody.getElementById(FILES_TO_ADD_DIV_ID);
		if(null!=divFilesToAdd){
			Elements filesToAdd = divFilesToAdd.getElementsByAttributeValueStarting(ID_ATTRIBUTE, FILE_TO_ADD_START_ID);
			for(Element singleFile : filesToAdd){
				String id = singleFile.attr("id");
				String[] values = id.substring(id.indexOf(FILE_TO_ADD_START_ID)+4).split("_");
				PreviewFileAsset p = new PreviewFileAsset();
				p.setInode(values[0]);
				p.setParent(values[1]);
				p.setContentlet(Boolean.parseBoolean(values[2]));
				result.add(p);
			}
		}
		return result;
	}

	// ************************************************************************************************************
	// *************************************** BEGIN UTILITY METHODS JSOUP ****************************************
	// ************************************************************************************************************

	private static void removeAddContainer(Document templateBody){
		Elements addContainers = templateBody.getElementsByClass(ADD_CONTAINER_SPAN_CLASS);
		for(Element singleDiv : addContainers){
			singleDiv.remove();
		}
	}

	private static void removeMockContainers(Document templateBody){
		Elements mockContainers = templateBody.getElementsByClass(TITLE_CONTAINER_SPAN_CLASS);
		for(Element singleDiv : mockContainers){
			singleDiv.remove();
		}
	}

	private static void removeYuiGridContent(Document templateBody){
		Elements h1 = templateBody.getElementsByTag(H1_TAG);
		for(Element singleH1 : h1){
			singleH1.remove();
		}
	}

	private static void removeFileIconDiv(Document templateBody){
		Element divFilesIcons = templateBody.getElementById(FILE_CONTAINER_DIV_ID);
		if(null!=divFilesIcons)
			divFilesIcons.remove();
	}

	private static void getParseContainer(Document templateBody){
		Elements divHiddenParseContainer = templateBody.getElementsByAttributeValue(STYLE_ATTRIBUTE, STYLE_DISPLAY_NONE);
		for(Element singleDiv : divHiddenParseContainer){
			if(!singleDiv.attr(ID_ATTRIBUTE).equals("metatagToAdd")){
				Element parent = singleDiv.parent();
				if(!parent.attr(ID_ATTRIBUTE).equals("metatagToAdd")){
					String html = singleDiv.html();
					singleDiv.remove();
					parent.append(html);
				}
			}
		}
	}

	private static void addJsCssFiles(Document templateBody){
		Element head = templateBody.head();
		Element divFilesToAdd = templateBody.getElementById(FILES_TO_ADD_DIV_ID);
		if(null!=divFilesToAdd){
			Elements filesToAdd = divFilesToAdd.getElementsByAttributeValueStarting(ID_ATTRIBUTE, FILE_TO_ADD_START_ID);
			for(Element singleFile : filesToAdd){
				head.append(replaceHTMLComments(singleFile.html()));
				singleFile.remove();
			}
			divFilesToAdd.remove();
		}
	}

	private static void addHeadCode(Document templateBody, String headCode){
		Element head = templateBody.head();
		head.append(headCode);
	}

	private static void addHeaderCode(Document templateBody, String headCode){
		Element header = templateBody.getElementById(HEADER_ID);
		header.append(headCode);
	}

	private static void addFooterCode(Document templateBody, String headCode){
		Element header = templateBody.getElementById(FOOTER_ID);
		header.append(headCode);
	}

	private static String getPageWithValue(Document templateDrawedBody){
		Element globalContainer = templateDrawedBody.getElementsByAttributeValue(NAME_ATTRIBUTE, MAIN_DIV_NAME_VALUE).get(0);
		return globalContainer.attr(ID_ATTRIBUTE);
	}

	private static String getLayout(Document templateDrawedBody){
		Elements layouts = templateDrawedBody.getElementsByAttributeValue(ID_ATTRIBUTE, SIDEBAR_ID);
		if(null!=layouts && layouts.size()>0){
			Element layout = layouts.get(0);
			if(null!=layout)
				return templateDrawedBody.getElementsByAttributeValue(NAME_ATTRIBUTE, MAIN_DIV_NAME_VALUE).get(0).attr(CLASS_ATTRIBUTE);
			else
				return NO_SIDEBAR_VALUE;
		}else
			return NO_SIDEBAR_VALUE;
	}

	private static boolean hasHeader(Document templateDrawedBody){
		Element header = templateDrawedBody.getElementById(HEADER_ID);
		return header!=null;
	}

	private static boolean hasFooter(Document templateDrawedBody){
		Element footer = templateDrawedBody.getElementById(FOOTER_ID);
		return footer!=null;
	}

	private static List<TemplateLayoutRow> getSelectForBody(Document templateDrawedBody){
		List<TemplateLayoutRow> splitBodiesList = new ArrayList<TemplateLayoutRow>();
		Elements splitBodies = templateDrawedBody.select(DIV_TAG+"["+ID_ATTRIBUTE+"~="+getRegexForSelectBody());
		for(int i=0; i<splitBodies.size(); i++){
			Element splitBody = splitBodies.get(i);
			// gets the identifier of the body div
			String idHtml = splitBody.attr(ID_ATTRIBUTE);
			String id = idHtml.substring(idHtml.indexOf(SPLIT_BODY_ID_PREFIX)+SPLIT_BODY_ID_PREFIX.length());
			TemplateLayoutRow sb = new TemplateLayoutRow();
			sb.setIdentifier(Integer.parseInt(id));
			sb.setId("select_splitBody");
			sb.setValue(splitBody.child(0).attr(ID_ATTRIBUTE));
			splitBodiesList.add(sb);
		}
		return splitBodiesList;
	}

	private static void getMetatagContainers(Document templateBody){
		Element head = templateBody.head();
		Element metatagToAdd = templateBody.getElementById("metatagToAdd");
		if(null!=metatagToAdd){
			Elements metatags = metatagToAdd.getElementsByAttributeValueStarting(ID_ATTRIBUTE, FILE_TO_ADD_START_ID);
			for(Element meta:metatags)
				head.append(meta.html());
			metatagToAdd.remove();
		}
	}

	// **********************************************************************************************************
	// *************************************** END UTILITY METHODS JSOUP ****************************************
	// **********************************************************************************************************

	private static String replaceHTMLComments(String aHtml){
		return aHtml.substring(aHtml.indexOf(START_COMMENT)+5,aHtml.lastIndexOf(END_COMMENT));
	}

	private static String getRegexForSelectBody(){
		return "^["+SPLIT_BODY_ID_PREFIX+"]*[0-9]{1,}$";
	}

//	private static String getRegexForHrefReplace(){
//		return "[href=][\"/_-.0-9a-zA-z\"]{1,}";
//	}
}

