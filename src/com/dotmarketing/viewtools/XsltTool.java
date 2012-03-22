package com.dotmarketing.viewtools;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.velocity.context.Context;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;
import com.dotmarketing.viewtools.bean.XSLTranformationDoc;
import com.dotmarketing.viewtools.cache.XSLTransformationCache;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.files.business.FileAPI;
import com.dotmarketing.portlets.files.model.File;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;

/**
 * XSLTTransform macro methods
 * @author Oswaldo
 *
 *
 */
public class XsltTool implements ViewTool {

	private static final FileAPI fileAPI = APILocator.getFileAPI();
	private static final UserAPI userAPI = APILocator.getUserAPI();
	private HttpServletRequest request;
	private HostWebAPI hostWebAPI; // DOTCMS - 3800
	Context ctx;

	public void init(Object obj) {
		ViewContext context = (ViewContext) obj;
		this.request = context.getRequest();
		ctx = context.getVelocityContext();
		this.hostWebAPI = WebAPILocator.getHostWebAPI();
	}

	
	
	public String transform(String XMLPath, String XSLPath, int ttl) throws Exception {
		String x = XSLTTransform(XMLPath, XSLPath, ttl).getXmlTransformation();
		return x;
	
	}
	
	
	
	
	
	/**
	 * Transform the XML into the string according to the specification of the xsl file
	 * @param XMLPath Location of the XML file
	 * @param XSLPath Location of the XSL file
	 * @param ttl Time to Live
	 * @throws TransformerConfigurationException 
	 * @throws DotSecurityException 
	 * @throws DotDataException 
	 * @throws SystemException 
	 * @throws PortalException 
	 */
	public XSLTranformationDoc XSLTTransform(String XMLPath, String XSLPath, long ttl) throws Exception {


			String outputXML = null;
			Source xmlSource = null;
			XSLTranformationDoc doc = null;
			Host host = hostWebAPI.getCurrentHost(request);

			/*Validate if in cache exists a valid version*/
			doc = XSLTransformationCache.getXSLTranformationDocByXMLPath(XMLPath,XSLPath);

			if(doc == null){
				/*Get the XSL source*/
				java.io.File binFile = null;
				Identifier xslId = APILocator.getIdentifierAPI().find(host, XSLPath);
				if(xslId!=null && InodeUtils.isSet(xslId.getId()) && xslId.getAssetType().equals("contentlet")){
					Contentlet cont = APILocator.getContentletAPI().findContentletByIdentifier(xslId.getId(), true, APILocator.getLanguageAPI().getDefaultLanguage().getId(), userAPI.getSystemUser(),false);
					if(cont!=null && InodeUtils.isSet(cont.getInode())){
						binFile = cont.getBinary(FileAssetAPI.BINARY_FIELD);
					}
				}else{
					File xslFile = fileAPI.getFileByURI(XSLPath, host, true, userAPI.getSystemUser(),false);
					binFile = fileAPI.getAssetIOFile (xslFile);
				} 
				
				
				/*Get the XML Source from file or from URL*/
				if(!XMLPath.startsWith("http")){
					Identifier xmlId = APILocator.getIdentifierAPI().find(host, XMLPath);
					if(xmlId!=null && InodeUtils.isSet(xmlId.getId()) && xmlId.getAssetType().equals("contentlet")){
						Contentlet cont = APILocator.getContentletAPI().findContentletByIdentifier(xmlId.getId(), true, APILocator.getLanguageAPI().getDefaultLanguage().getId(), userAPI.getSystemUser(),false);
						if(cont!=null && InodeUtils.isSet(cont.getInode())){
							xmlSource = new StreamSource(new InputStreamReader(new FileInputStream(cont.getBinary(FileAssetAPI.BINARY_FIELD)), "UTF8"));
						}
					}else{
						File xmlFile = fileAPI.getFileByURI(XMLPath, host, true,userAPI.getSystemUser(),false);
						xmlSource = new StreamSource(new InputStreamReader(new FileInputStream(fileAPI.getAssetIOFile(xmlFile)), "UTF8"));
					}

				}else{
					xmlSource = new StreamSource(XMLPath);
				}

				Source xsltSource = new StreamSource(new InputStreamReader(new FileInputStream(binFile), "UTF8"));

				// create an instance of TransformerFactory
				TransformerFactory transFact = TransformerFactory.newInstance();
				StreamResult result = new StreamResult(new ByteArrayOutputStream());
				Transformer trans = transFact.newTransformer(xsltSource);

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
				doc.setXslPath(XSLPath);
				doc.setXmlPath(XMLPath);
				doc.setXmlTransformation(outputXML);
				doc.setTtl(new Date().getTime()+ttl);

				XSLTransformationCache.addXSLTranformationDoc(doc);

			}

			return doc;

	}
	
	/**
	 * Transform the XML into the string according to the specification of the xsl file
	 * @param XMLString String in XML format
	 * @param XSLPath Location of the XSL file
	 * @param ttl Time to Live
	 */
	public XSLTranformationDoc XSLTTransformXMLString(String xmlString, String XSLPath) {
		try {
			String outputXML = null;
			Source xmlSource = null;
			XSLTranformationDoc doc = null;
			Host host = hostWebAPI.getCurrentHost(request);
			
			/*Get the XSL source*/
			File xslFile = fileAPI.getFileByURI(XSLPath, host, true, userAPI.getSystemUser(), false);
			
			if (doc == null) {
				xmlSource = new StreamSource(new ByteArrayInputStream(xmlString.getBytes("UTF-8")));
				
				Source xsltSource = new StreamSource(new InputStreamReader(new FileInputStream(fileAPI.getAssetIOFile (xslFile)), "UTF8"));
				
				// create an instance of TransformerFactory
				TransformerFactory transFact = TransformerFactory.newInstance();
				StreamResult result = new StreamResult(new ByteArrayOutputStream());
				Transformer trans = transFact.newTransformer(xsltSource);
				
				try {
					trans.transform(xmlSource, result);
				} catch (Exception e1) {
					Logger.error(XsltTool.class, "Error in transformation. " + e1.getMessage());
					e1.printStackTrace();
				}
				
				outputXML = result.getOutputStream().toString();
				
				doc = new XSLTranformationDoc();
				doc.setIdentifier(xslFile.getIdentifier());
				doc.setInode(xslFile.getInode());
				doc.setXslPath(XSLPath);
				doc.setXmlTransformation(outputXML);
			}
			
			return doc;
		} catch (Exception e) {
			Logger.error(XsltTool.class, "Error in transformation. " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}
}
