package com.dotcms.rendering.velocity.viewtools;

import com.dotcms.cost.RequestCost;
import com.dotcms.cost.RequestPrices.Price;
import com.dotcms.featureflag.FeatureFlagName;
import com.dotcms.http.CircuitBreakerUrl;
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
import io.vavr.Lazy;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Date;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;
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

	/** Stylesheet used to find out which capabilities the XSLT engine is running with. */
	private static final String CAPABILITY_PROBE =
			"<xsl:stylesheet version='1.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'"
					+ " xmlns:bool='http://xml.apache.org/xalan/java/java.lang.Boolean'>"
					+ "<xsl:template match='/'><out>"
					+ "<xsl:value-of select=\"bool:toString(true())\"/>"
					+ "</out></xsl:template></xsl:stylesheet>";

	/** What the probe stylesheet writes when the engine lets it call out of XSLT. */
	private static final String CAPABILITY_PROBE_OUTPUT = ">true<";

	/** The engine the probe stylesheet is written for; see {@link #extensionFunctionsStillRun()}. */
	private static final String JDK_TRANSFORMER_FACTORY_PREFIX = "com.sun.org.apache.xalan.internal.xsltc";

	/** Checks the engine on first use and warns when it is not running with the expected settings. */
	private static final Lazy<Boolean> ENGINE_CAPABILITY_CHECK = Lazy.of(() -> {
		if (extensionFunctionsStillRun()) {
			Logger.warn(XsltTool.class,
					"XSLTTool: the XSLT engine is not running with the restrictions this tool configures; "
							+ "check the XML processing settings of this JVM.");
		}
		return true;
	});
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
    @RequestCost(Price.XSLT_PARSE)
	public XSLTranformationDoc xslTransform(String xmlPath, String xslPath, long ttl)
			throws DotDataException, DotSecurityException, SystemException, PortalException, IOException, TransformerConfigurationException {

			if(!canUserEvaluate()){
				Logger.error(XsltTool.class, "XSLTTool user does not have scripting access ");
				return null;
			}

			if (isRemoteSource(xmlPath) && !Config.getBooleanProperty(
					FeatureFlagName.FEATURE_FLAG_XSLT_REMOTE_XML, false)) {
				Logger.warn(XsltTool.class, remoteSourceDisabledMessage(xmlPath));
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

    @RequestCost(Price.XSLT_FETCH_AND_PARSE)
	private XSLTranformationDoc getXslTransformationDoc(final String xmlPath, final String xslPath, final long ttl,
			final Identifier xslId, final File binFile, final Source xmlSource)
			throws IOException, TransformerConfigurationException {


		final Source xsltSource = new StreamSource(new InputStreamReader(Files.newInputStream(binFile.toPath()), StandardCharsets.UTF_8));

		final TransformerFactory factory = restrictedTransformerFactory();

		StreamResult result = new StreamResult(new ByteArrayOutputStream());
		Transformer trans = factory.newTransformer(xsltSource);

		boolean transformed = true;
		try{
			trans.transform(xmlSource, result);
		}catch(Exception e1){
			transformed = false;
			Logger.error(XsltTool.class, "Error in transformation of [" + xslPath + "]. ", e1);
		}

		final String outputXML = result.getOutputStream().toString();

		final XSLTranformationDoc doc = new XSLTranformationDoc();
		doc.setIdentifier(xslId.getId());
		doc.setInode(xslId.getInode());
		doc.setXslPath(xslPath);
		doc.setXmlPath(xmlPath);
		doc.setXmlTransformation(outputXML);
		doc.setTtl(new Date().getTime()+ ttl);

		// A transformation that failed produces no output; caching it would keep serving the empty
		// result for the whole TTL instead of trying again on the next render.
		if (transformed) {
			XSLTransformationCache.addXSLTranformationDoc(doc);
		}
		return doc;
	}

	/**
	 * Builds the {@link TransformerFactory} used to compile and run stylesheets, restricted to what
	 * a transformation actually needs: no external DTDs, no external stylesheets, and secure
	 * processing on, which is what keeps a stylesheet from calling Java methods through XSLT
	 * extension functions.
	 *
	 * <p>The settings are applied here rather than left to the runtime, whose defaults for XML
	 * processing have changed across Java versions, so the tool behaves the same wherever it is
	 * deployed.
	 *
	 * @return a factory that only does XSLT
	 * @throws TransformerConfigurationException if the factory does not support secure processing
	 */
	static TransformerFactory restrictedTransformerFactory() throws TransformerConfigurationException {
		ENGINE_CAPABILITY_CHECK.get();
		return newRestrictedFactory();
	}

	/**
	 * Creates the restricted factory. Kept apart from {@link #restrictedTransformerFactory()} so the
	 * boot-time probe can build one without re-entering the probe itself.
	 *
	 * @return a factory configured with secure processing and no external access
	 * @throws TransformerConfigurationException if the factory does not support secure processing
	 */
	private static TransformerFactory newRestrictedFactory() throws TransformerConfigurationException {
		final TransformerFactory factory = TransformerFactory.newInstance();
		// Secure processing keeps a stylesheet within XSLT.
		factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		// Disable access to external entities in XML parsing.
		factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
		factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
		return factory;
	}

	/**
	 * Transforms a tiny document with a stylesheet that calls a Java method, and reports whether the
	 * call went through.
	 *
	 * <p>It reports what the engine does rather than what the configuration asks for, since JVM
	 * properties take precedence over it. The stylesheet is written for the JDK engine, so another
	 * engine is reported as unreadable rather than guessed at. Used on first use, for the warning
	 * above.
	 *
	 * @return true when the call ran, which means the configured settings are not the ones in effect
	 */
	static boolean extensionFunctionsStillRun() {
		final StringWriter output = new StringWriter();
		final TransformerFactory factory;
		try {
			factory = newRestrictedFactory();
		} catch (Exception | TransformerFactoryConfigurationError e) {
			// newInstance() reports a missing or unusable engine as an Error, not an Exception.
			Logger.warn(XsltTool.class,
					"XSLTTool could not check how the XSLT engine is configured: " + e.getMessage(), e);
			return false;
		}

		if (!factory.getClass().getName().startsWith(JDK_TRANSFORMER_FACTORY_PREFIX)) {
			// The probe stylesheet is written for the JDK engine; another one cannot be read with it.
			Logger.warn(XsltTool.class, "XSLTTool is running on the XSLT engine ["
					+ factory.getClass().getName() + "], whose configuration this tool cannot verify");
			return false;
		}

		try {
			factory.newTransformer(new StreamSource(new StringReader(CAPABILITY_PROBE)))
					.transform(new StreamSource(new StringReader("<probe/>")), new StreamResult(output));
		} catch (TransformerException e) {
			// The engine turned the call down, which is what we want.
			return false;
		} catch (Exception e) {
			Logger.warn(XsltTool.class,
					"XSLTTool could not check how the XSLT engine is configured: " + e.getMessage(), e);
			return false;
		}

		return output.toString().contains(CAPABILITY_PROBE_OUTPUT);
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
			return loadRemoteXML(xmlPath);
		}
		return Optional.empty();
	}

	/**
	 * Downloads an XML document that the template asked for by URL.
	 *
	 * <p>Turned off by default: the download only happens when
	 * {@link FeatureFlagName#FEATURE_FLAG_XSLT_REMOTE_XML} is enabled, and it goes through
	 * {@link CircuitBreakerUrl}, which applies the platform's outbound network policy, stops waiting
	 * after
	 * {@code XSLT_REMOTE_XML_TIMEOUT_MS} and stops reading after {@code XSLT_REMOTE_XML_MAX_BYTES}.
	 * The document is read into memory and handed to the transformer as text, so the XSLT engine
	 * never opens a connection of its own.
	 *
	 * <p>Anything other than a successful response — an error status, a timeout, a refused host, a
	 * document over the size limit — returns empty rather than the bytes that arrived, so a failed
	 * download cannot end up transformed and cached as if it were the document.
	 *
	 * @param xmlPath the URL the template passed as the XML source
	 * @return the downloaded document, or empty when remote sources are off or the download failed
	 * @deprecated taking the XML from an arbitrary URL is on its way out. Store the document in
	 * dotCMS and pass its path instead.
	 */
	/**
	 * Tells whether the template asked for the XML by URL rather than by a path inside dotCMS.
	 *
	 * @param xmlPath what the template passed as the XML source
	 * @return true when it is a URL
	 */
	static boolean isRemoteSource(final String xmlPath) {
		return xmlPath != null && xmlPath.startsWith("http");
	}

	/**
	 * Explains, for the log, why a URL was not read, and what to do about it.
	 *
	 * @param xmlPath the URL the template passed as the XML source
	 * @return the message, naming both the URL and the setting that allows it
	 */
	static String remoteSourceDisabledMessage(final String xmlPath) {
		return String.format(
				"XSLTTool was given the URL [%s] as the XML source. Reading XML from a URL is "
						+ "deprecated and disabled; set %s to true to allow it, or store the document "
						+ "in dotCMS and pass its path.",
				xmlPath, FeatureFlagName.FEATURE_FLAG_XSLT_REMOTE_XML);
	}

	@Deprecated
	Optional<Source> loadRemoteXML(final String xmlPath) {
		if (!Config.getBooleanProperty(FeatureFlagName.FEATURE_FLAG_XSLT_REMOTE_XML, false)) {
			Logger.warn(XsltTool.class, remoteSourceDisabledMessage(xmlPath));
			return Optional.empty();
		}

		Logger.warn(XsltTool.class, "XSLTTool is reading XML from a URL, which is deprecated");

		final CircuitBreakerUrl remoteCall = CircuitBreakerUrl.builder()
				.setUrl(xmlPath)
				.setTimeout(Config.getIntProperty("XSLT_REMOTE_XML_TIMEOUT_MS", 2000))
				.setMaxResponseBytes(Config.getIntProperty("XSLT_REMOTE_XML_MAX_BYTES", 5 * 1024 * 1024))
				.setRaiseFailsafe(true)
				.build();

		final byte[] document;
		try (final ByteArrayOutputStream downloaded = new ByteArrayOutputStream()) {
			remoteCall.doOut(downloaded);
			document = downloaded.toByteArray();
		} catch (Exception e) {
			Logger.warn(XsltTool.class, String.format(
					"XSLTTool could not read the XML at [%s]: %s", xmlPath, e.getMessage()), e);
			return Optional.empty();
		}

		if (remoteCall.isError() || document.length == 0) {
			Logger.warn(XsltTool.class, String.format(
					"XSLTTool got no usable XML from [%s], response [%d]", xmlPath, remoteCall.response()));
			return Optional.empty();
		}

		// Bytes rather than text, so the document's own encoding declaration is still honoured.
		return Optional.of(new StreamSource(new ByteArrayInputStream(document)));
	}

	protected boolean canUserEvaluate() {
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
