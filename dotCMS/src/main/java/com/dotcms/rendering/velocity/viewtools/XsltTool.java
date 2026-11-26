package com.dotcms.rendering.velocity.viewtools;

import com.dotcms.rendering.velocity.viewtools.bean.XSLTranformationDoc;
import com.dotcms.rendering.velocity.viewtools.cache.XSLTransformationCache;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
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
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Date;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.velocity.context.Context;
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

	
	
	public String transform(String xmlPath, String xslPath, int ttl)
			throws SystemException, TransformerConfigurationException, DotDataException, DotSecurityException, PortalException, IOException {
		return xslTransform(xmlPath, xslPath, ttl).getXmlTransformation();

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
	public XSLTranformationDoc xslTransform(String xmlPath, String xslPath, long ttl)
			throws DotDataException, DotSecurityException, SystemException, PortalException, IOException, TransformerConfigurationException {

			if(!canUserEvaluate()){
				Logger.error(XsltTool.class, "XSLTTool user does not have scripting access ");
				return null;
			}

			final Host site = hostWebAPI.getCurrentHost(request);

			/*Validate if in cache exists a valid version*/
		    final XSLTranformationDoc doc =  XSLTransformationCache.getXSLTranformationDocByXMLPath(xmlPath,xslPath);

			if(doc != null){
			   return doc;
			}
				/*Get the XSL source*/
		        final Identifier xslId = APILocator.getIdentifierAPI().find(site, xslPath);
				final Optional<File> binFile = loadXSL(xslId);

				if(binFile.isEmpty()){
					final String errorMessage = String.format("Fail retrieving xsl file from site [%s] using path [%s]", site.getName(), xslPath);
					Logger.error(XsltTool.class, errorMessage);
					throw new IllegalArgumentException(errorMessage);
				}

				final Optional<Source> xmlSource = loadXML(site, xmlPath);
				if(xmlSource.isEmpty()){
					final String errorMessage = String.format("Fail retrieving xml file from site [%s] using path [%s]", site.getName(), xmlPath);
					Logger.error(XsltTool.class, errorMessage);
					throw new IllegalArgumentException(errorMessage);
				}

		return getXslTransformationDoc(xmlPath, xslPath, ttl, xslId, binFile.get(), xmlSource.get());


	}

	private XSLTranformationDoc getXslTransformationDoc(final String xmlPath, final String xslPath, final long ttl,
			final Identifier xslId, final File binFile, final Source xmlSource)
			throws IOException, TransformerConfigurationException {


		final Source xsltSource = new StreamSource(new InputStreamReader(Files.newInputStream(binFile.toPath()), StandardCharsets.UTF_8));

		// create an instance of TransformerFactory
		TransformerFactory factory = TransformerFactory.newInstance();
		// Secure processing turns off the XSLT extension functions (java: / xalan: calls), which would
		// otherwise let a stylesheet instantiate arbitrary Java classes and run code on the server.
		factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		//Disable access to external entities in XML parsing.
		factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
		factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");

		StreamResult result = new StreamResult(new ByteArrayOutputStream());
		Transformer trans = factory.newTransformer(xsltSource);

		try{
			trans.transform(xmlSource, result);
		}catch(Exception e1){
			Logger.error(XsltTool.class, "Error in transformation. ", e1);
		}

		final String outputXML = result.getOutputStream().toString();

		final XSLTranformationDoc doc = new XSLTranformationDoc();
		doc.setIdentifier(xslId.getId());
		doc.setInode(xslId.getInode());
		doc.setXslPath(xslPath);
		doc.setXmlPath(xmlPath);
		doc.setXmlTransformation(outputXML);
		doc.setTtl(new Date().getTime()+ ttl);

		XSLTransformationCache.addXSLTranformationDoc(doc);
		return doc;
	}

	private Optional<File> loadXSL(final Identifier xslId)
			throws DotDataException, DotSecurityException, IOException {
		if (xslId != null && InodeUtils.isSet(xslId.getId()) && xslId.getAssetType().equals("contentlet")) {
			Contentlet cont = APILocator.getContentletAPI()
					.findContentletByIdentifier(xslId.getId(), true,
							APILocator.getLanguageAPI().getDefaultLanguage().getId(),
							userAPI.getSystemUser(), false);
			if (cont != null && InodeUtils.isSet(cont.getInode())) {
				return Optional.of(cont.getBinary(FileAssetAPI.BINARY_FIELD));
			}
		}
		return Optional.empty();
	}

	private Optional<Source> loadXML(final Host site, final String xmlPath)
			throws IOException, DotDataException, DotSecurityException {
		if (!xmlPath.startsWith("http")) {
			Identifier xmlId = APILocator.getIdentifierAPI().find(site, xmlPath);
			if (xmlId != null && InodeUtils.isSet(xmlId.getId()) && xmlId.getAssetType()
					.equals("contentlet")) {
				Contentlet cont = APILocator.getContentletAPI()
						.findContentletByIdentifier(xmlId.getId(), true,
								APILocator.getLanguageAPI().getDefaultLanguage().getId(),
								userAPI.getSystemUser(), false);
				if (cont != null && InodeUtils.isSet(cont.getInode())) {
					return Optional.of(new StreamSource(new InputStreamReader(Files.newInputStream(
							cont.getBinary(FileAssetAPI.BINARY_FIELD).toPath()),
							StandardCharsets.UTF_8)));
				}
			}

		} else {
			return Optional.of(new StreamSource(xmlPath));
		}
		return Optional.empty();
	}

	/**
	 * Decides whether the code that is calling this tool may run it.
	 *
	 * <p>Two conditions, and both are about the installation and the author rather than about the
	 * user reading the page: scripting has to be switched on, and the content whose code calls
	 * this tool has to have been last saved by someone holding the
	 * {@link com.dotmarketing.business.Role#SCRIPTING_DEVELOPER} role.
	 *
	 * <p>Who that author is comes from the request, where the render that knows what content it
	 * is rendering puts it. It used to be read off the current Velocity resource name, which
	 * cannot work: {@code #parse} pushes whatever string the template author wrote as the current
	 * template name, so the caller got to name the user it was checked against, and the names
	 * dotCMS really produces ({@code /LIVE/{id}/1.container}, {@code /EDIT_MODE/{id}/{id}.fields})
	 * do not even parse, which denied every legitimate caller as well.
	 *
	 * @return {@code true} when this tool may run
	 */
	protected boolean canUserEvaluate() {
		if (!Config.getBooleanProperty("ENABLE_SCRIPTING", false)) {
			Logger.warn(this.getClass(), "Scripting called and ENABLE_SCRIPTING set to false");
			return false;
		}
		return RestrictedViewToolAccess.renderingAuthorHasAccess(this.request);
	}

}
