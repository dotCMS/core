package com.dotmarketing.services;

import bsh.This;
import com.dotcms.enterprise.LicenseUtil;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpages.business.HTMLPageAPI;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.*;
import com.dotmarketing.velocity.DotResourceCache;
import org.apache.velocity.runtime.resource.ResourceManager;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.sql.Timestamp;
import java.util.*;

/**
 * @author will
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class PageServices {

	public static void invalidate(HTMLPage htmlPage) throws DotStateException, DotDataException {

		Identifier identifier = APILocator.getIdentifierAPI().find(htmlPage);
		invalidate(htmlPage, identifier, false);
		invalidate(htmlPage, identifier, true);

	}

	public static void invalidate(HTMLPage htmlPage, boolean EDIT_MODE) throws DotStateException, DotDataException {

		Identifier identifier = APILocator.getIdentifierAPI().find(htmlPage);
		invalidate(htmlPage, identifier, EDIT_MODE);
	}

	public static void invalidate(HTMLPage htmlPage, Identifier identifier, boolean EDIT_MODE) {
		removePageFile(htmlPage, identifier, EDIT_MODE);
	}

	public static InputStream buildStream(HTMLPage htmlPage, boolean EDIT_MODE) throws DotStateException, DotDataException {
		Identifier identifier = APILocator.getIdentifierAPI().find(htmlPage);
		try{
			return buildStream(htmlPage, identifier, EDIT_MODE);
		}
		catch(Exception e){
			Logger.error(PageServices.class, e.getMessage(),e);	
			throw new DotRuntimeException(e.getMessage());
		}
	}

	@SuppressWarnings("unchecked")
	public static InputStream buildStream(HTMLPage htmlPage, Identifier identifier, boolean EDIT_MODE) throws DotDataException, DotSecurityException {
		String folderPath = (!EDIT_MODE) ? "live/" : "working/";
		InputStream result;
		StringBuilder sb = new StringBuilder();

		ContentletAPI conAPI = APILocator.getContentletAPI();
		Template cmsTemplate = com.dotmarketing.portlets.htmlpages.factories.HTMLPageFactory.getHTMLPageTemplate(htmlPage, EDIT_MODE);
		if(cmsTemplate == null || ! InodeUtils.isSet(cmsTemplate.getInode())){
			Logger.error(This.class, "PAGE DOES NOT HAVE A VALID TEMPLATE (template unpublished?) : page id " + htmlPage.getIdentifier() + ":" + identifier.getURI()   );
		}
		
		//gets pageChannel for this path
		java.util.StringTokenizer st = new java.util.StringTokenizer(String.valueOf(identifier.getURI()),"/");
		String pageChannel = null;
		if(st.hasMoreTokens()){
			pageChannel = st.nextToken();
		}
		
		
		
		
		// set the page cache var
		if(htmlPage.getCacheTTL() > 0 && LicenseUtil.getLevel() > 99){
			sb.append("#set($dotPageCacheDate = \"").append( new java.util.Date() ).append("\")");
			sb.append("#set($dotPageCacheTTL = \"").append( htmlPage.getCacheTTL()  ).append("\")");
		}
		
		
		
		// set the host variables
		HTMLPageAPI htmlPageAPI = APILocator.getHTMLPageAPI();

		Host host = htmlPageAPI.getParentHost(htmlPage);
		sb.append("#if(!$doNotParseTemplate)");
			sb.append("$velutil.mergeTemplate('" ).append( folderPath ).append( host.getIdentifier() ).append( "." ).append( Config.getStringProperty("VELOCITY_HOST_EXTENSION") ).append( "')");
		sb.append(" #end ");
		
		
		
		//creates the context where to place the variables
		// Build a context to pass to the page
		sb.append("#if(!$doNotSetPageInfo)");
		sb.append("#set ( $quote = '\"' )");
		sb.append("#set ($HTMLPAGE_INODE = \"" ).append( String.valueOf(htmlPage.getInode()) ).append( "\" )");
		sb.append("#set ($HTMLPAGE_IDENTIFIER = \"" ).append( String.valueOf(htmlPage.getIdentifier()) ).append( "\" )");
		sb.append("#set ($HTMLPAGE_TITLE = \"" ).append( UtilMethods.espaceForVelocity(htmlPage.getTitle()) ).append( "\" )");
		sb.append("#set ($HTMLPAGE_FRIENDLY_NAME = \"" + UtilMethods.espaceForVelocity(htmlPage.getFriendlyName()) ).append( "\" )");
		sb.append("#set ($TEMPLATE_INODE = \"" ).append( String.valueOf(cmsTemplate.getInode()) ).append( "\" )");
		sb.append("#set ($HTMLPAGE_META = \"" ).append( UtilMethods.espaceForVelocity(htmlPage.getMetadata()) ).append( "\" )");
		sb.append("#set ($HTMLPAGE_META = \"#fixBreaks($HTMLPAGE_META)\")");
		
		sb.append("#set ($HTMLPAGE_DESCRIPTION = \"" ).append( UtilMethods.espaceForVelocity(htmlPage.getSeoDescription()) ).append( "\" )");
		sb.append("#set ($HTMLPAGE_DESCRIPTION = \"#fixBreaks($HTMLPAGE_DESCRIPTION)\")");
		
		sb.append("#set ($HTMLPAGE_KEYWORDS = \"" ).append( UtilMethods.espaceForVelocity(htmlPage.getSeoKeywords()) ).append( "\" )");
		sb.append("#set ($HTMLPAGE_KEYWORDS = \"#fixBreaks($HTMLPAGE_KEYWORDS)\")");
		
		
		sb.append("#set ($HTMLPAGE_SECURE = \"" ).append( String.valueOf(htmlPage.isHttpsRequired()) ).append( "\" )");
		sb.append("#set ($VTLSERVLET_URI = \"" ).append( UtilMethods.encodeURIComponent(identifier.getURI()) ).append( "\" )");
		sb.append("#set ($HTMLPAGE_REDIRECT = \"" ).append( UtilMethods.espaceForVelocity(htmlPage.getRedirect()) ).append( "\" )");
		
		sb.append("#set ($pageTitle = \"" ).append( UtilMethods.espaceForVelocity(htmlPage.getTitle()) ).append( "\" )");
		sb.append("#set ($pageChannel = \"" ).append( pageChannel ).append( "\" )");
		sb.append("#set ($friendlyName = \"" ).append( UtilMethods.espaceForVelocity(htmlPage.getFriendlyName()) ).append( "\" )");

		Date moddate = null;
		if(UtilMethods.isSet(htmlPage.getModDate())){
			moddate = htmlPage.getModDate();
		} else {
			moddate = htmlPage.getStartDate();
		}

		moddate = new Timestamp(moddate.getTime());

		sb.append("#set ($HTML_PAGE_LAST_MOD_DATE= $date.toDate(\"yyyy-MM-dd HH:mm:ss.SSS\", \"" ).append( moddate ).append( "\"))");
		sb.append("#set ($HTMLPAGE_MOD_DATE= $date.toDate(\"yyyy-MM-dd HH:mm:ss.SSS\", \"" ).append( moddate ).append( "\"))");
		sb.append(" #end ");
						
		//get the containers for the page and stick them in context
		//List identifiers = InodeFactory.getChildrenClass(cmsTemplate, Identifier.class);

        List<Container> containerList = APILocator.getTemplateAPI().getContainersInTemplate(cmsTemplate, APILocator.getUserAPI().getSystemUser(), false);

		Iterator i = containerList.iterator();
		while(i.hasNext()){
			Container ident = (Container) i.next();
			
			Container c = null;
			if (EDIT_MODE) {
				c = (Container) APILocator.getVersionableAPI().findWorkingVersion(ident.getIdentifier(),APILocator.getUserAPI().getSystemUser(),false);
			}
			else {
				c = (Container) APILocator.getVersionableAPI().findLiveVersion(ident.getIdentifier(),APILocator.getUserAPI().getSystemUser(),false);
			}
			//sets container to load the container file
			sb.append("#set ($container").append(ident.getIdentifier() ).append( " = \"" ).append( folderPath ).append( ident.getIdentifier() ).append( "." ).append( Config.getStringProperty("VELOCITY_CONTAINER_EXTENSION") ).append( "\" )");

			String sort = (c.getSortContentletsBy() == null) ? "tree_order" : c.getSortContentletsBy();

			boolean dynamicContainer = UtilMethods.isSet(c.getLuceneQuery());

			int langCounter = 0;


			List<Contentlet> contentlets = new ArrayList<Contentlet>();
			if (!dynamicContainer) {
				Identifier idenHtmlPage = APILocator.getIdentifierAPI().find(htmlPage);
				Identifier idenContainer = APILocator.getIdentifierAPI().find(c);
				//The container doesn't have categories
				try{
					contentlets = conAPI.findPageContentlets(idenHtmlPage.getId(), idenContainer.getId(), sort, EDIT_MODE, -1,APILocator.getUserAPI().getSystemUser() ,false);
				}catch(Exception e){
					Logger.error(PageServices.class,"Unable to retrive contentlets on page", e);
				}
				Logger.debug(PageServices.class, "HTMLPage= " + htmlPage.getInode() + " Container=" + c.getInode() + " Language=-1 Contentlets=" + contentlets.size());
			}
			//this is to filter the contentlets list removing the repited identifiers
			if(contentlets.size() > 0){
				Set<String> contentletIdentList = new HashSet<String>();
				List<Contentlet> contentletsFilter = new ArrayList<Contentlet>();
				for(Contentlet cont : contentlets){
					if(!contentletIdentList.contains(cont.getIdentifier())){
						contentletIdentList.add(cont.getIdentifier());
						contentletsFilter.add(cont);
					}
				}
				contentlets = contentletsFilter;
			}

			StringBuilder contentletList = new StringBuilder();
			Iterator iter = contentlets.iterator();
			int count = 0;
			while (iter.hasNext() && count < c.getMaxContentlets()) {
				Contentlet contentlet = (Contentlet) iter.next();
				Identifier contentletIdentifier;
				try {
					contentletIdentifier = APILocator.getIdentifierAPI().find(contentlet);
				} catch (DotHibernateException dhe) {
					contentletIdentifier = new Identifier();
					Logger.error(PageServices.class,"Unable to rertive identifier for contentlet",dhe);
				}

					contentletList.append("\"").append(contentletIdentifier.getInode()).append("\"");
					if (iter.hasNext() && count < c.getMaxContentlets()) {
						contentletList.append(",");	
					}
				count++;
				Structure contStructure =contentlet.getStructure();
				if(contStructure.getStructureType()== Structure.STRUCTURE_TYPE_WIDGET){
					Field field=contStructure.getFieldVar("widgetPreexecute");
					if (field!= null && UtilMethods.isSet(field.getValues())) {
						sb.append(field.getValues().trim());
					}
				}
				
				
			}
			sb.append("#set ($contentletList" ).append( ident.getIdentifier() ).append( " = [" ).append( contentletList.toString() ).append( "] )");
			sb.append("#set ($totalSize" ).append( ident.getIdentifier() ).append( "=" ).append( count ).append( ")");
			langCounter++;


		}

		if(htmlPage.isHttpsRequired()){		
			sb.append(" #if(!$ADMIN_MODE  && !$request.isSecure())");
			sb.append("    #if($request.getQueryString())");
			sb.append("        #set ($REDIRECT_URL = \"https://${request.getServerName()}$request.getAttribute('javax.servlet.forward.request_uri')?$request.getQueryString()\")");
			sb.append("    #else ");
			sb.append("        #set ($REDIRECT_URL = \"https://${request.getServerName()}$request.getAttribute('javax.servlet.forward.request_uri')\")");
			sb.append("    #end ");
			sb.append("    $response.sendRedirect(\"$REDIRECT_URL\")"); 
			sb.append(" #end ");
		}
		
		sb.append("#if($HTMLPAGE_REDIRECT != \"\")");
		sb.append("    $response.sendRedirect(\"$HTMLPAGE_REDIRECT\")");
		sb.append("#end");
		
		Identifier iden = APILocator.getIdentifierAPI().find(cmsTemplate);

		
		sb.append("#if(!$doNotParseTemplate)");
            if ( cmsTemplate.isDrawed() ) {//We have a designed template
                //Setting some theme variables
                sb.append( "#set ($dotTheme = $templatetool.theme(\"" ).append( cmsTemplate.getTheme() ).append( "\",\"" ).append( host.getIdentifier() ).append( "\"))" );
                sb.append( "#set ($dotThemeLayout = $templatetool.themeLayout(\"" ).append( cmsTemplate.getInode() ).append( "\" ))" );
                //Merging our template
                sb.append( "$velutil.mergeTemplate(\"$dotTheme.templatePath\")" );
            } else {
                sb.append( "$velutil.mergeTemplate('" ).append( folderPath ).append( iden.getInode() ).append( "." ).append( Config.getStringProperty( "VELOCITY_TEMPLATE_EXTENSION" ) ).append( "')" );
            }
		sb.append("#end");
		
		
		try {

			if(Config.getBooleanProperty("SHOW_VELOCITYFILES", false)){
			    String realFolderPath = (!EDIT_MODE) ? "live" + java.io.File.separator: "working" + java.io.File.separator;
	            String velocityRootPath = Config.getStringProperty("VELOCITY_ROOT");
	            String filePath = realFolderPath + identifier.getInode() + "." + Config.getStringProperty("VELOCITY_HTMLPAGE_EXTENSION");
	            if (velocityRootPath.startsWith("/WEB-INF")) {
	                velocityRootPath = Config.CONTEXT.getRealPath(velocityRootPath);
	            }
	            velocityRootPath += java.io.File.separator;
			    
				java.io.BufferedOutputStream tmpOut = new java.io.BufferedOutputStream(new java.io.FileOutputStream(new java.io.File(ConfigUtils.getDynamicVelocityPath()+java.io.File.separator + filePath)));
				//Specify a proper character encoding
				OutputStreamWriter out = new OutputStreamWriter(tmpOut, UtilMethods.getCharsetConfiguration());

				out.write(sb.toString());

				out.flush();
				out.close();
				tmpOut.close();
			}
		} catch (Exception e) {
			Logger.error(PageServices.class, e.toString(), e);
		}
		try {
			result = new ByteArrayInputStream(sb.toString().getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e1) {
			result = new ByteArrayInputStream(sb.toString().getBytes());
			Logger.error(ContainerServices.class,e1.getMessage(), e1);
		}
		return result;
	}

	public static void unpublishPageFile(HTMLPage htmlPage) throws DotStateException, DotDataException {

		Identifier identifier = APILocator.getIdentifierAPI().find(htmlPage);
		removePageFile(htmlPage, identifier, false);
	}

	public static void removePageFile(HTMLPage htmlPage, boolean EDIT_MODE) throws DotStateException, DotDataException {

		Identifier identifier = APILocator.getIdentifierAPI().find(htmlPage);
		removePageFile(htmlPage, identifier, EDIT_MODE);
	}

	public static void removePageFile (HTMLPage htmlPage, Identifier identifier, boolean EDIT_MODE) {
		String folderPath = (!EDIT_MODE) ? "live" + java.io.File.separator: "working" + java.io.File.separator;
		String velocityRootPath = Config.getStringProperty("VELOCITY_ROOT");
		if (velocityRootPath.startsWith("/WEB-INF")) {
			velocityRootPath = Config.CONTEXT.getRealPath(velocityRootPath);
		}
		String filePath = folderPath + identifier.getInode() + "." + Config.getStringProperty("VELOCITY_HTMLPAGE_EXTENSION");
		velocityRootPath += java.io.File.separator;
		java.io.File f  = new java.io.File(velocityRootPath + filePath);
		f.delete();
		DotResourceCache vc = CacheLocator.getVeloctyResourceCache();
		vc.remove(ResourceManager.RESOURCE_TEMPLATE + filePath );
		CacheLocator.getHTMLPageCache().remove((HTMLPage) htmlPage);
	}

}
