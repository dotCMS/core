package com.dotcms.publisher.util;

import java.io.File;
import java.io.FileInputStream;
import java.net.Socket;
import java.security.KeyStore;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

public class TrustFactory {
	private static final String truststore_path = Config.getStringProperty("TRUSTSTORE_PATH", null); //cacerts.jks";
    private static final String truststore_password = Config.getStringProperty("TRUSTSTORE_PWD", null);
    private static final String keystore_path = Config.getStringProperty("KEYSTORE_PATH", null); //keystore.jks";
    private static final String keystore_password = Config.getStringProperty("KEYSTORE_PWD", null);
	
	public SSLContext getSSLContext() {
        TrustManager mytm[] = null;
        KeyManager mykm[] = null;

        try {
        	if(UtilMethods.isSet(truststore_path) && UtilMethods.isSet(truststore_password) ){
        		mytm = new TrustManager[]{new MyX509TrustManager(truststore_path, truststore_password.toCharArray())};
        	}
            
            if(UtilMethods.isSet(keystore_path) && UtilMethods.isSet(keystore_password) ){
            	mykm = new KeyManager[]{new MyX509KeyManager(keystore_path, keystore_password.toCharArray())};
            
            }
        } catch (Exception ex) {
            Logger.error(this.getClass(), ex.toString());
            Logger.debug(this.getClass(), ex.getMessage(), ex);
        }

        SSLContext ctx = null;
        try {
            ctx = SSLContext.getInstance("SSL");
            ctx.init(mykm, mytm, null);
        } catch (java.security.GeneralSecurityException ex) {
            Logger.error(this.getClass(), ex.getMessage(), ex);

            
        }
        return ctx;
    }
	
	public HostnameVerifier getHostnameVerifier() {
        return new HostnameVerifier() {

            @Override
            public boolean verify(String hostname, javax.net.ssl.SSLSession sslSession) {
                return true;
            }
        };
    }
	
	static class MyX509TrustManager implements X509TrustManager {

        /*
         * The default PKIX X509TrustManager9.  We'll delegate
         * decisions to it, and fall back to the logic in this class if the
         * default X509TrustManager doesn't trust it.
         */
        X509TrustManager pkixTrustManager;

        MyX509TrustManager(String trustStore, char[] password) throws Exception {
            this(new File(trustStore), password);
        }

        MyX509TrustManager(File trustStore, char[] password) throws Exception {
            // create a "default" JSSE X509TrustManager.

            KeyStore ks = KeyStore.getInstance("JKS");

            ks.load(new FileInputStream(trustStore), password);

            TrustManagerFactory tmf = TrustManagerFactory.getInstance("PKIX");
            tmf.init(ks);

            TrustManager tms[] = tmf.getTrustManagers();

            /*
             * Iterate over the returned trustmanagers, look
             * for an instance of X509TrustManager.  If found,
             * use that as our "default" trust manager.
             */
            for (int i = 0; i < tms.length; i++) {
                if (tms[i] instanceof X509TrustManager) {
                    pkixTrustManager = (X509TrustManager) tms[i];
                    return;
                }
            }

            /*
             * Find some other way to initialize, or else we have to fail the
             * constructor.
             */
            throw new Exception("Couldn't initialize");
        }

        /*
         * Delegate to the default trust manager.
         */
        public void checkClientTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
            try {
                pkixTrustManager.checkClientTrusted(chain, authType);
            } catch (CertificateException excep) {
                Logger.error(this.getClass(), excep.getMessage());
                Logger.debug(this.getClass(), excep.getMessage(), excep);
            }
        }

        /*
         * Delegate to the default trust manager.
         */
        public void checkServerTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
            try {
                pkixTrustManager.checkServerTrusted(chain, authType);
            } catch (CertificateException excep) {
                Logger.error(this.getClass(), excep.getMessage());
                Logger.debug(this.getClass(), excep.getMessage(), excep);
                throw excep;
            }
        }

        /*
         * Merely pass this through.
         */
        public X509Certificate[] getAcceptedIssuers() {
            return pkixTrustManager.getAcceptedIssuers();
        }
    }
	
	static class MyX509KeyManager implements X509KeyManager {

        /*
         * The default PKIX X509KeyManager.  We'll delegate
         * decisions to it, and fall back to the logic in this class if the
         * default X509KeyManager doesn't trust it.
         */
        X509KeyManager pkixKeyManager;

        MyX509KeyManager(String keyStore, char[] password) throws Exception {
            this(new File(keyStore), password);
        }

        MyX509KeyManager(File keyStore, char[] password) throws Exception {
            // create a "default" JSSE X509KeyManager.

            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(new FileInputStream(keyStore), password);

            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509", "SunJSSE");
            kmf.init(ks, password);

            KeyManager kms[] = kmf.getKeyManagers();

            /*
             * Iterate over the returned keymanagers, look
             * for an instance of X509KeyManager.  If found,
             * use that as our "default" key manager.
             */
            for (int i = 0; i < kms.length; i++) {
                if (kms[i] instanceof X509KeyManager) {
                    pkixKeyManager = (X509KeyManager) kms[i];
                    return;
                }
            }

            /*
             * Find some other way to initialize, or else we have to fail the
             * constructor.
             */
            throw new Exception("Couldn't initialize");
        }

        public PrivateKey getPrivateKey(String arg0) {
            return pkixKeyManager.getPrivateKey(arg0);
        }

        public X509Certificate[] getCertificateChain(String arg0) {
            return pkixKeyManager.getCertificateChain(arg0);
        }

        public String[] getClientAliases(String arg0, Principal[] arg1) {
            return pkixKeyManager.getClientAliases(arg0, arg1);
        }

        public String chooseClientAlias(String[] arg0, Principal[] arg1, Socket arg2) {
            return pkixKeyManager.chooseClientAlias(arg0, arg1, arg2);
        }

        public String[] getServerAliases(String arg0, Principal[] arg1) {
            return pkixKeyManager.getServerAliases(arg0, arg1);
        }

        public String chooseServerAlias(String arg0, Principal[] arg1, Socket arg2) {
            return pkixKeyManager.chooseServerAlias(arg0, arg1, arg2);
        }
    }
}