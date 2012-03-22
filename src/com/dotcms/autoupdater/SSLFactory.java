package com.dotcms.autoupdater;


 
 import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.ControllerThreadSocketFactory;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;
import org.apache.log4j.Logger;
import com.dotcms.autoupdater.AuthSSLInitializationError;
import com.dotcms.autoupdater.AuthSSLX509TrustManager;
import com.dotcms.autoupdater.Messages;
import com.dotcms.autoupdater.UpdateAgent;

import com.sun.net.ssl.KeyManager;
import com.sun.net.ssl.KeyManagerFactory;
import com.sun.net.ssl.SSLContext;
import com.sun.net.ssl.TrustManager;
import com.sun.net.ssl.TrustManagerFactory;
import com.sun.net.ssl.X509TrustManager;

	  


	  
	  public class SSLFactory implements ProtocolSocketFactory {
	  
	      /** Log object for this class. */
		  private static final Logger LOG = Logger.getLogger(UpdateAgent.class);
	  
	      private URL keystoreUrl = null;
	      private String keystorePassword = null;
	      private URL truststoreUrl = null;
	      private String truststorePassword = null;
	      private SSLContext sslcontext = null;
	  
	      /**
	       * Constructor for AuthSSLProtocolSocketFactory. Either a keystore or truststore file
	       * must be given. Otherwise SSL context initialization error will result.
	       * 
	       * @param keystoreUrl URL of the keystore file. May be <tt>null</tt> if HTTPS client
	       *        authentication is not to be used.
	       * @param keystorePassword Password to unlock the keystore. IMPORTANT: this implementation
	       *        assumes that the same password is used to protect the key and the keystore itself.
	       * @param truststoreUrl URL of the truststore file. May be <tt>null</tt> if HTTPS server
	       *        authentication is not to be used.
	       * @param truststorePassword Password to unlock the truststore.
	       */
	      public SSLFactory(
	          final URL keystoreUrl, final String keystorePassword, 
	          final URL truststoreUrl, final String truststorePassword)
	      {
	          super();
	          this.keystoreUrl = keystoreUrl;
	          this.keystorePassword = keystorePassword;
	          this.truststoreUrl = truststoreUrl;
	          this.truststorePassword = truststorePassword;
	      }
	  
	      private static KeyStore createKeyStore(final URL url, final String password) 
	          throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException
	      {
	          if (url == null) {
	              throw new IllegalArgumentException(Messages.getString("SSLFactory.error.null.keystore.url")); //$NON-NLS-1$
	          }
	          LOG.debug(Messages.getString("SSLFactory.debug.init.keystore")); //$NON-NLS-1$
	          KeyStore keystore  = KeyStore.getInstance("jks"); //$NON-NLS-1$
	          keystore.load(url.openStream(), password != null ? password.toCharArray(): null);
	          return keystore;
	      }
	      
	      private static KeyManager[] createKeyManagers(final KeyStore keystore, final String password)
	          throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException 
	      {
	          if (keystore == null) {
	              throw new IllegalArgumentException(Messages.getString("SSLFactory.error.null.keystore")); //$NON-NLS-1$
	          }
	          LOG.debug(Messages.getString("SSLFactory.debut.init.key.manager")); //$NON-NLS-1$
	          KeyManagerFactory kmfactory = KeyManagerFactory.getInstance(
	              KeyManagerFactory.getDefaultAlgorithm());
	          kmfactory.init(keystore, password != null ? password.toCharArray(): null);
	          return kmfactory.getKeyManagers(); 
	      }
	  
	      private static TrustManager[] createTrustManagers(final KeyStore keystore)
	          throws KeyStoreException, NoSuchAlgorithmException
	      { 
	          if (keystore == null) {
	              throw new IllegalArgumentException(Messages.getString("SSLFactory.error.null.keystore")); //$NON-NLS-1$
	          }
	          LOG.debug(Messages.getString("SSLFactory.init.trust.manager")); //$NON-NLS-1$
	          TrustManagerFactory tmfactory = TrustManagerFactory.getInstance(
	              TrustManagerFactory.getDefaultAlgorithm());
	          tmfactory.init(keystore);
	          TrustManager[] trustmanagers = tmfactory.getTrustManagers();
	          for (int i = 0; i < trustmanagers.length; i++) {
	              if (trustmanagers[i] instanceof X509TrustManager) {
	                  trustmanagers[i] = new AuthSSLX509TrustManager(
	                      (X509TrustManager)trustmanagers[i]); 
	              }
	          }
	          return trustmanagers; 
	      }
	  
	      private SSLContext createSSLContext() {
	          try {
	              KeyManager[] keymanagers = null;
	              TrustManager[] trustmanagers = null;
	              if (this.keystoreUrl != null) {
	                  KeyStore keystore = createKeyStore(this.keystoreUrl, this.keystorePassword);
	                  if (LOG.isDebugEnabled()) {
	                      Enumeration aliases = keystore.aliases();
	                      while (aliases.hasMoreElements()) {
	                          String alias = (String)aliases.nextElement();                        
	                          Certificate[] certs = keystore.getCertificateChain(alias);
	                          if (certs != null) {
	                              LOG.debug("Certificate chain '" + alias + "':"); //$NON-NLS-1$ //$NON-NLS-2$
	                              for (int c = 0; c < certs.length; c++) {
	                                  if (certs[c] instanceof X509Certificate) {
	                                      X509Certificate cert = (X509Certificate)certs[c];
	                                      LOG.debug(" Certificate " + (c + 1) + ":"); //$NON-NLS-1$ //$NON-NLS-2$
	                                      LOG.debug("  Subject DN: " + cert.getSubjectDN()); //$NON-NLS-1$
	                                      LOG.debug("  Signature Algorithm: " + cert.getSigAlgName()); //$NON-NLS-1$
	                                      LOG.debug("  Valid from: " + cert.getNotBefore() ); //$NON-NLS-1$
	                                      LOG.debug("  Valid until: " + cert.getNotAfter()); //$NON-NLS-1$
	                                      LOG.debug("  Issuer: " + cert.getIssuerDN()); //$NON-NLS-1$
	                                  }
	                              }
	                          }
	                      }
	                  }
	                  keymanagers = createKeyManagers(keystore, this.keystorePassword);
	              }
	              if (this.truststoreUrl != null) {
	                  KeyStore keystore = createKeyStore(this.truststoreUrl, this.truststorePassword);
	                  if (LOG.isDebugEnabled()) {
	                      Enumeration aliases = keystore.aliases();
	                      while (aliases.hasMoreElements()) {
	                          String alias = (String)aliases.nextElement();
	                          LOG.debug("Trusted certificate '" + alias + "':"); //$NON-NLS-1$ //$NON-NLS-2$
	                          Certificate trustedcert = keystore.getCertificate(alias);
	                          if (trustedcert != null && trustedcert instanceof X509Certificate) {
	                              X509Certificate cert = (X509Certificate)trustedcert;
	                              LOG.debug("  Subject DN: " + cert.getSubjectDN()); //$NON-NLS-1$
	                              LOG.debug("  Signature Algorithm: " + cert.getSigAlgName()); //$NON-NLS-1$
	                              LOG.debug("  Valid from: " + cert.getNotBefore() ); //$NON-NLS-1$
	                              LOG.debug("  Valid until: " + cert.getNotAfter()); //$NON-NLS-1$
	                              LOG.debug("  Issuer: " + cert.getIssuerDN()); //$NON-NLS-1$
	                          }
	                      }
	                  }
	                  trustmanagers = createTrustManagers(keystore);
	              }
	              SSLContext sslcontext = SSLContext.getInstance("SSL"); //$NON-NLS-1$
	              sslcontext.init(keymanagers, trustmanagers, null);
	              return sslcontext;
	          } catch (NoSuchAlgorithmException e) {
	              LOG.error(e.getMessage(), e);
	              throw new AuthSSLInitializationError("Unsupported algorithm exception: " + e.getMessage()); //$NON-NLS-1$
	          } catch (KeyStoreException e) {
	              LOG.error(e.getMessage(), e);
	              throw new AuthSSLInitializationError("Keystore exception: " + e.getMessage()); //$NON-NLS-1$
	          } catch (GeneralSecurityException e) {
	              LOG.error(e.getMessage(), e);
	              throw new AuthSSLInitializationError("Key management exception: " + e.getMessage()); //$NON-NLS-1$
	          } catch (IOException e) {
	              LOG.error(e.getMessage(), e);
	              throw new AuthSSLInitializationError("I/O error reading keystore/truststore file: " + e.getMessage()); //$NON-NLS-1$
	          }
	      }
	  
	      private SSLContext getSSLContext() {
	          if (this.sslcontext == null) {
	              this.sslcontext = createSSLContext();
	          }
	          return this.sslcontext;
	      }
	  
	      /**
	       * Attempts to get a new socket connection to the given host within the given time limit.
	       * <p>
	       * To circumvent the limitations of older JREs that do not support connect timeout a 
	       * controller thread is executed. The controller thread attempts to create a new socket 
	       * within the given limit of time. If socket constructor does not return until the 
	       * timeout expires, the controller terminates and throws an {@link ConnectTimeoutException}
	       * </p>
	       *  
	       * @param host the host name/IP
	       * @param port the port on the host
	       * @param clientHost the local host name/IP to bind the socket to
	       * @param clientPort the port on the local machine
	       * @param params {@link HttpConnectionParams Http connection parameters}
	       * 
	       * @return Socket a new socket
	       * 
	       * @throws IOException if an I/O error occurs while creating the socket
	       * @throws UnknownHostException if the IP address of the host cannot be
	       * determined
	       */
	      public Socket createSocket(
	          final String host,
	          final int port,
	          final InetAddress localAddress,
	          final int localPort,
	          final HttpConnectionParams params
	      ) throws IOException, UnknownHostException, ConnectTimeoutException {
	         if (params == null) {
	              throw new IllegalArgumentException(Messages.getString("SSLFactory.parameter.null")); //$NON-NLS-1$
	          }
	          int timeout = params.getConnectionTimeout();
	          if (timeout == 0) {
	              return createSocket(host, port, localAddress, localPort);
	          } else {
	              // To be eventually deprecated when migrated to Java 1.4 or above
	              return ControllerThreadSocketFactory.createSocket(
	                      this, host, port, localAddress, localPort, timeout);
	          }
	      }
	  
	      /**
	       * @see SecureProtocolSocketFactory#createSocket(java.lang.String,int,java.net.InetAddress,int)
	       */
	      public Socket createSocket(
	          String host,
	          int port,
	          InetAddress clientHost,
	          int clientPort)
	          throws IOException, UnknownHostException
	     {
	         return getSSLContext().getSocketFactory().createSocket(
	              host,
	              port,
	              clientHost,
	              clientPort
	          );
	      }
	  
	      /**
	       * @see SecureProtocolSocketFactory#createSocket(java.lang.String,int)
	       */
	      public Socket createSocket(String host, int port)
	          throws IOException, UnknownHostException
	      {
	          return getSSLContext().getSocketFactory().createSocket(
	              host,
	              port
	          );
	      }
	  
	      /**
	       * @see SecureProtocolSocketFactory#createSocket(java.net.Socket,java.lang.String,int,boolean)
	       */
	      public Socket createSocket(
	          Socket socket,
	          String host,
	          int port,
	          boolean autoClose)
	          throws IOException, UnknownHostException
	      {
	          return getSSLContext().getSocketFactory().createSocket(
	              socket,
	              host,
	              port,
	              autoClose
	          );
	      }
	  }

