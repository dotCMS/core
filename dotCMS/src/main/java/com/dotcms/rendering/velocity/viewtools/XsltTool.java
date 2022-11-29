package com.dotcms.rendering.velocity.viewtools;

import com.dotcms.rendering.velocity.viewtools.bean.XSLTranformationDoc;
import com.dotcms.rendering.velocity.viewtools.cache.XSLTransformationCache;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Date;
import javax.servlet.http.HttpServletRequest;
import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.velocity.context.Context;
import org.apache.velocity.context.InternalContextAdapterImpl;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

/**
 * XSLTTransform macro methods
 * @author Oswaldo
 *
 *
 */
public class XsltTool implements ViewTool {

	private static final UserAPI userAPI = APILocator.getUserAPI();
	private HttpServletRequest request;
	private HostWebAPI hostWebAPI; 
	protected Host host;
	Context ctx;
	private InternalContextAdapterImpl ica;
	protected User user = null;
	protected User backuser = null;
	protected boolean respectFrontendRoles = false;
	protected UserWebAPI userWebAPI;
	public void init(Object obj) {
		if(!Config.getBooleanProperty("ENABLE_SCRIPTING", false)){
			return;
		}
		ViewContext context = (ViewContext) obj;
		
		this.request = context.getRequest();
		ctx = context.getVelocityContext();		
		try {
			host = WebAPILocator.getHostWebAPI().getCurrentHost(request);
		} catch (PortalException e1) {
			Logger.error(this,e1.getMessage(),e1);
		} catch (SystemException e1) {
			Logger.error(this,e1.getMessage(),e1);
		} catch (DotDataException e1) {
			Logger.error(this,e1.getMessage(),e1);
		} catch (DotSecurityException e1) {
			Logger.error(this,e1.getMessage(),e1);
		}
		userWebAPI = WebAPILocator.getUserWebAPI();
		try {
			user = userWebAPI.getLoggedInFrontendUser(request);
			backuser = userWebAPI.getLoggedInUser(request);
			respectFrontendRoles = true;
		} catch (Exception e) {
			Logger.error(this, "Error finding the logged in user", e);
		}


		this.hostWebAPI = WebAPILocator.getHostWebAPI();
	}

	
	
	public String transform(String xmlPath, String xslPath, int ttl) throws Exception {
		return XSLTTransform(xmlPath, xslPath, ttl).getXmlTransformation();
	}
	
	
	
	
	
	/**
	 * Transform the XML into the string according to the specification of the xsl file
	 * @param xmlPath Location of the XML file
	 * @param xslPath Location of the XSL file
	 * @param ttl Time to Live
	 * @throws TransformerConfigurationException 
	 * @throws DotSecurityException 
	 * @throws DotDataException 
	 * @throws SystemException 
	 * @throws PortalException 
	 */
	public XSLTranformationDoc XSLTTransform(String xmlPath, String xslPath, long ttl) throws Exception {

			if(!canUserEvalute()){
				Logger.error(XsltTool.class, "XSLTTool user does not have scripting access ");
				return null;
			}
			String outputXML = null;
			Source xmlSource = null;
			XSLTranformationDoc doc = null;
			final Host site = hostWebAPI.getCurrentHost(request);

			/*Validate if in cache exists a valid version*/
			doc = XSLTransformationCache.getXSLTranformationDocByXMLPath(xmlPath,xslPath);

			if(doc == null){
				/*Get the XSL source*/
				java.io.File binFile = null;
				Identifier xslId = APILocator.getIdentifierAPI().find(site, xslPath);
				if(xslId!=null && InodeUtils.isSet(xslId.getId()) && xslId.getAssetType().equals("contentlet")){
					Contentlet cont = APILocator.getContentletAPI().findContentletByIdentifier(xslId.getId(), true, APILocator.getLanguageAPI().getDefaultLanguage().getId(), userAPI.getSystemUser(),false);
					if(cont!=null && InodeUtils.isSet(cont.getInode())){
						binFile = cont.getBinary(FileAssetAPI.BINARY_FIELD);
					}
				}
				
				
				/*Get the XML Source from file or from URL*/
				if(!xmlPath.startsWith("http")){
					Identifier xmlId = APILocator.getIdentifierAPI().find(site, xmlPath);
					if(xmlId!=null && InodeUtils.isSet(xmlId.getId()) && xmlId.getAssetType().equals("contentlet")){
						Contentlet cont = APILocator.getContentletAPI().findContentletByIdentifier(xmlId.getId(), true, APILocator.getLanguageAPI().getDefaultLanguage().getId(), userAPI.getSystemUser(),false);
						if(cont!=null && InodeUtils.isSet(cont.getInode())){
							xmlSource = new StreamSource(new InputStreamReader(Files.newInputStream(
									cont.getBinary(FileAssetAPI.BINARY_FIELD).toPath()),
									StandardCharsets.UTF_8));
						}
					}

				}else{
					xmlSource = new StreamSource(xmlPath);
				}

				final Source xsltSource = new StreamSource(
						new InputStreamReader(Files.newInputStream(binFile.toPath()),
								StandardCharsets.UTF_8));

				// create an instance of TransformerFactory
				TransformerFactory factory = TransformerFactory.newInstance();
				//Disable access to external entities in XML parsing.
				factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
				factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");

				StreamResult result = new StreamResult(new ByteArrayOutputStream());
				Transformer trans = factory.newTransformer(xsltSource);

				try{
					trans.transform(xmlSource, result);
				}catch(Exception e1){
					Logger.error(XsltTool.class, "Error in transformation. "+e1.getMessage());
					e1.printStackTrace();
				}

				outputXML = result.getOutputStream().toString();

				doc = new XSLTranformationDoc();
				doc.setIdentifier(xslId.getId());
				doc.setInode(xslId.getInode());
				doc.setXslPath(xslPath);
				doc.setXmlPath(xmlPath);
				doc.setXmlTransformation(outputXML);
				doc.setTtl(new Date().getTime()+ttl);

				XSLTransformationCache.addXSLTranformationDoc(doc);

			}

			return doc;

	}
	
	protected boolean canUserEvalute() throws DotDataException, DotSecurityException{
		if(!Config.getBooleanProperty("ENABLE_SCRIPTING", false)){
			Logger.warn(this.getClass(), "Scripting called and ENABLE_SCRIPTING set to false");
			return false;
		}
		try{
		
			ica = new InternalContextAdapterImpl(ctx);
			String fieldResourceName = ica.getCurrentTemplateName();
			String conInode = fieldResourceName.substring(fieldResourceName.indexOf("/") + 1, fieldResourceName.indexOf("_"));
			
			Contentlet con = APILocator.getContentletAPI().find(conInode, APILocator.getUserAPI().getSystemUser(), true);
			
			
			User mu = userAPI.loadUserById(con.getModUser(), APILocator.getUserAPI().getSystemUser(), true);
			Role scripting =APILocator.getRoleAPI().loadRoleByKey("Scripting Developer");
			return APILocator.getRoleAPI().doesUserHaveRole(mu, scripting);
		}
		catch(Exception e){
			Logger.warn(this.getClass(), "Scripting called with error" + e);
			return false;	
			
		}
	}
	
}
