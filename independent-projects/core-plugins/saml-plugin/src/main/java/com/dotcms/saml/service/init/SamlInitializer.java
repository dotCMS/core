package com.dotcms.saml.service.init;

import com.dotcms.saml.utils.SignatureUtils;
import net.shibboleth.utilities.java.support.xml.BasicParserPool;
import org.opensaml.core.config.InitializationException;
import org.opensaml.core.config.InitializationService;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.xmlsec.config.JavaCryptoValidationInitializer;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import java.security.Provider;
import java.security.Security;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Default initializer Responsibilities: - Init the Java Crypto. - Init Saml
 * Services. - Init Plugin Configuration and meta data.
 *
 * @author jsanca
 */
public class SamlInitializer implements Initializer {

	private static final long serialVersionUID = -5869927082029401479L;

	private final static AtomicBoolean initDone = new AtomicBoolean( false );

	public SamlInitializer() {
	}

	@Override
	public synchronized void init(final Map<String, Object> context) {


		final ClassLoader  threadLoader = Thread.currentThread().getContextClassLoader();

		try {
			Thread.currentThread().setContextClassLoader(InitializationService.class.getClassLoader());

			Logger.info(this.getClass().getName(),"SAML Init STARTED..." );
			
	        InitializationService.initialize();
			new org.opensaml.xmlsec.config.JavaCryptoValidationInitializer().init();

			Logger.info(this.getClass().getName(),"Doing instance of Java Crypto validator");
			Logger.info(this.getClass().getName(),"Getting the Security Providers");
	        for (final Provider jceProvider : Security.getProviders()) {
	            Logger.info(this.getClass().getName(),"- " + jceProvider.getInfo());
	        }

			Logger.info(this.getClass().getName(),"Doing Init of SignatureValidationProvider" );
			SignatureUtils.init();

	        Logger.info(this.getClass().getName(),"SAML Init DONE" );
		} catch (final InitializationException e) {
			throw new RuntimeException( "Initialization failed", e );
		} finally {
		    Thread.currentThread().setContextClassLoader(threadLoader);
		}

		Logger.info(this.getClass().getName(),"Setting basic parser pool");

		if (XMLObjectProviderRegistrySupport.getParserPool() == null ) {
			XMLObjectProviderRegistrySupport.setParserPool(new BasicParserPool());
		}

		




		initDone.set( true );
	}

	@Override
	public boolean isInitializationDone() {

		return initDone.get();
	}

}
