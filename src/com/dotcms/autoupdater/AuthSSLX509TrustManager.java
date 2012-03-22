package com.dotcms.autoupdater;

import java.security.cert.X509Certificate;

import org.apache.log4j.Logger;
import com.dotcms.autoupdater.UpdateAgent;

import com.sun.net.ssl.X509TrustManager;

 
 public class AuthSSLX509TrustManager implements X509TrustManager
 {
     private X509TrustManager defaultTrustManager = null;
 
     /** Log object for this class. */
     private static final Logger LOG = Logger.getLogger(UpdateAgent.class);
 

     public AuthSSLX509TrustManager(final X509TrustManager defaultTrustManager) {
         super();
         if (defaultTrustManager == null) {
             throw new IllegalArgumentException("Trust manager may not be null");
         }
         this.defaultTrustManager = defaultTrustManager;
     }
 
     /**
      * @see com.sun.net.ssl.X509TrustManager#isClientTrusted(X509Certificate[])
      */
     public boolean isClientTrusted(X509Certificate[] certificates) {
         if (LOG.isInfoEnabled() && certificates != null) {
             for (int c = 0; c < certificates.length; c++) {
                 X509Certificate cert = certificates[c];
                 LOG.debug(" Client certificate " + (c + 1) + ":");
                 LOG.debug("  Subject DN: " + cert.getSubjectDN());
                 LOG.debug("  Signature Algorithm: " + cert.getSigAlgName());
                 LOG.debug("  Valid from: " + cert.getNotBefore() );
                 LOG.debug("  Valid until: " + cert.getNotAfter());
                 LOG.debug("  Issuer: " + cert.getIssuerDN());
             }
         }
         return this.defaultTrustManager.isClientTrusted(certificates);
     }
 
     /**
      * @see com.sun.net.ssl.X509TrustManager#isServerTrusted(X509Certificate[])
      */
     public boolean isServerTrusted(X509Certificate[] certificates) {
         if (LOG.isInfoEnabled() && certificates != null) {
             for (int c = 0; c < certificates.length; c++) {
                 X509Certificate cert = certificates[c];
                 LOG.debug(" Server certificate " + (c + 1) + ":");
                 LOG.debug("  Subject DN: " + cert.getSubjectDN());
                 LOG.debug("  Signature Algorithm: " + cert.getSigAlgName());
                 LOG.debug("  Valid from: " + cert.getNotBefore() );
                 LOG.debug("  Valid until: " + cert.getNotAfter());
                 LOG.debug("  Issuer: " + cert.getIssuerDN());
             }
         }
         return this.defaultTrustManager.isServerTrusted(certificates);
     }
 
     /**
      * @see com.sun.net.ssl.X509TrustManager#getAcceptedIssuers()
      */
     public X509Certificate[] getAcceptedIssuers() {
         return this.defaultTrustManager.getAcceptedIssuers();
     }
 }