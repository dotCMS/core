package com.dotmarketing.portlets.templates.design.util;

import com.dotcms.rendering.velocity.directive.ParseContainer;
import com.dotcms.repackage.org.jsoup.Jsoup;
import com.dotcms.repackage.org.jsoup.nodes.Document;
import com.dotcms.repackage.org.jsoup.nodes.Element;
import com.dotcms.repackage.org.jsoup.select.Elements;
import com.dotcms.uuid.shorty.ShortType;
import com.dotcms.uuid.shorty.ShortyId;
import com.dotcms.uuid.shorty.ShortyIdAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.containers.business.FileAssetContainerUtil;
import com.dotmarketing.portlets.templates.design.bean.ContainerUUID;
import com.dotmarketing.portlets.templates.design.bean.PreviewFileAsset;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayoutRow;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.StringPool;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.dotmarketing.portlets.templates.design.util.DesignTemplateHtmlCssConstants.*;

/**
 * This class contains a list of utility's methods for the design of the template.
 *
 * @author	Graziano Aliberti - Engineering Ingegneria Informatica
 * @date	Apr 19, 2012
 */
public class DesignTemplateUtil {
	private static final Pattern parseContainerPatter = Pattern.compile( "(?<=#parseContainer\\(').*?(?='\\))" );

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
    public static TemplateLayout getDesignParameters ( String drawedBody ) {
        return getDesignParameters( drawedBody, false );
    }

    /**
     * Get the values for the design fields.
     *
     * @param drawedBody
     * @param isPreview
     * @return
     */
    public static TemplateLayout  getDesignParameters ( String drawedBody, Boolean isPreview ) {

        Document templateDrawedBody = Jsoup.parse( drawedBody );
        TemplateLayout parameters = new TemplateLayout();
        parameters.setPageWidth( getPageWithValue( templateDrawedBody ) );
        parameters.setHeader( hasHeader( templateDrawedBody ) );
        parameters.setFooter( hasFooter( templateDrawedBody ) );
        parameters.setLayout( getLayout( templateDrawedBody ) );
        //Set the body layout to the template
        setLayoutBody( parameters, templateDrawedBody, isPreview );

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
	    Elements globalContainers = templateDrawedBody.getElementsByAttributeValue(NAME_ATTRIBUTE, MAIN_DIV_NAME_VALUE);
        if(null!=globalContainers && !globalContainers.isEmpty()){
	        Element globalContainer = globalContainers.get(0);
	        return globalContainer.attr(ID_ATTRIBUTE);
	    } else {
            return "100%";
                    
        }
	}

	private static String getLayout(Document templateDrawedBody){
		Elements layouts = templateDrawedBody.getElementsByAttributeValue(ID_ATTRIBUTE, SIDEBAR_ID);
		if(null!=layouts && !layouts.isEmpty()){
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

    /**
     * Method that will parse the drawed body in order to split it in rows for the main column, also
     * will verify if the drawed body have a sidebar.
     * <p/>
     * After the parse will set the main column and the sidebar (if present) to the template layout.
     *
     * @param layout
     * @param templateDrawedBody
     * @param isPreview
     * @return
     */
    private static void setLayoutBody ( TemplateLayout layout, Document templateDrawedBody, Boolean isPreview ) {

        //***************************************************************
        //Verify if we have a sidebar
        Elements splitSideBar = templateDrawedBody.select( DIV_TAG + "[" + ID_ATTRIBUTE + "=" + SIDEBAR_ID );
        if ( splitSideBar != null && !splitSideBar.isEmpty() ) {//We found our sidebar

            Element sidebar = splitSideBar.get( 0 );

            //Getting the containers for this html fragment
            List<ContainerUUID> containers = getColumnContainers( sidebar );
            //Adding the sidebar to the layout
            layout.setContainers( containers, isPreview );
        }

        //***************************************************************
        //Split the drawed body in rows
        List<TemplateLayoutRow> splitBodiesList = new ArrayList<TemplateLayoutRow>();
        Elements splitBodies = templateDrawedBody.select( DIV_TAG + "[" + ID_ATTRIBUTE + "~=" + getRegexForSelectBody() );
        for ( int i = 0; i < splitBodies.size(); i++ ) {

            Element splitBody = splitBodies.get( i );
            // gets the identifier of the body div
            String idHtml = splitBody.attr( ID_ATTRIBUTE );
            String id = idHtml.substring( idHtml.indexOf( SPLIT_BODY_ID_PREFIX ) + SPLIT_BODY_ID_PREFIX.length() );
            String layoutType = splitBody.child( 0 ).attr( ID_ATTRIBUTE );

            //Create a template row
            TemplateLayoutRow rowLayout = new TemplateLayoutRow();
            rowLayout.setIdentifier( Integer.parseInt( id ) );
            rowLayout.setId( "select_splitBody" );
            rowLayout.setValue( layoutType );

            //We may have  multiple columns in here
            Elements columns = splitBody.select( DIV_TAG + "." + COLUMN_CONTAINER_CLASS );
            if ( columns != null && !columns.isEmpty() ) {

                //We found multiple columns...
                for ( Element columnElement : columns ) {
                    //Find the containers for this column
                    List<ContainerUUID> containers = getColumnContainers( columnElement );
                    //Adding the containers for this column
                    rowLayout.addColumnContainers( containers, isPreview );
                }
                //Add the created row
                splitBodiesList.add( rowLayout );

            } else { //It means we just have one column

                //Find the containers for this column
                List<ContainerUUID> containers = getColumnContainers( splitBody );
                rowLayout.addColumnContainers( containers, isPreview );
                //Add the created row
                splitBodiesList.add( rowLayout );
            }

        }
        //Set the body column with its rows
        layout.setBodyRows( splitBodiesList );
    }

    /**
     * Method that will parse and return the containers inside a given html fragment
     *
     * @param splitBody
     * @return
     */
    private static List<ContainerUUID> getColumnContainers (Element splitBody ) {

        //Getting the containers for this html fragment
        List<ContainerUUID> containers = new ArrayList<>();
        Matcher matcher = parseContainerPatter.matcher( splitBody.text() );
        while ( matcher.find() ) {
            String parseContainerArguments = matcher.group();

            if (parseContainerArguments != null) {
				String[] splitArguments = parseContainerArguments.split(",");
				String id = cleanId(splitArguments[0]);
				String uuid = splitArguments.length > 1 ? cleanId(splitArguments[1]) : ParseContainer.DEFAULT_UUID_VALUE;
				try {
					id = getContainerIdentifierOrPath(id);
				} catch (Exception e) {
					Logger.error(DesignTemplateUtil.class, e.getMessage());
				}

				containers.add(new ContainerUUID(id, uuid));
			}
        }

        return containers;
    }

	/**
	 * Checks if the identifier is a file asset container, if it is replace the id by the apth
	 * @param containerId String could be a path or an id (file asset id or db container id)
	 * @return String if it is a file asset container, returns the path, otherwise the uuid.
	 * @throws DotDataException
	 */
	public static String getContainerIdentifierOrPath(final String containerId) throws DotDataException {

		if (FileAssetContainerUtil.getInstance().isFolderAssetContainerId(containerId)) {

			return containerId;
		}

    	final ShortyIdAPI shortyIdAPI     = APILocator.getShortyAPI();
    	final IdentifierAPI identifierAPI = APILocator.getIdentifierAPI();
    	final Optional<ShortyId> shortyId = shortyIdAPI.getShorty(containerId);

    	if (!shortyId.isPresent()) {
    		return containerId;
		}

		return shortyId.get().subType == ShortType.CONTAINER ?
				containerId:
				FileAssetContainerUtil.getInstance().getFullPath(identifierAPI.find(containerId).getParentPath());
	}

	private static String cleanId(final String identifier) {

    	return StringUtils.remove(identifier, StringPool.APOSTROPHE);
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

