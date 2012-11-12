package com.dotcms.cli.security;


import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Collection;

public class KeyStoreImportKey  {
    
    private static InputStream fullStream ( String fname ) throws IOException {
        FileInputStream fis = new FileInputStream(fname);
        DataInputStream dis = new DataInputStream(fis);
        byte[] bytes = new byte[dis.available()];
        dis.readFully(bytes);
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        return bais;
    }
        
    /**
     * This main takes 5 parameters:
     *	 	-	keystore (absolute path);
     *	      -	keystore password;
     *		-	alias;
     *		-	keyfile;
     *		-	certfile;
    */
    public static void main ( String args[]) {
        if (args.length != 5) {
            System.out.println("Usage: java java com.dotcms.cli.security.KeyStoreImportKey keystore keystore_password alias keyfile certfile");
            System.exit(0);
	  }        

        String keypass = args[1];
        String alias = args[2];
        String keystorename = args[0];

        if (keystorename == null){
		System.out.println("Error: you must pass the keystore file");
            System.exit(0);		
	  }

        // parsing command line input
        String keyfile = args[3];
        String certfile = args[4];

        try {
            KeyStore ks = KeyStore.getInstance("JKS", "SUN");            
            System.out.println("Using keystore-file : "+keystorename);
            ks.load(new FileInputStream ( keystorename ),
                    keypass.toCharArray());

            // loading Key
            InputStream fl = fullStream (keyfile);
            byte[] key = new byte[fl.available()];
            KeyFactory kf = KeyFactory.getInstance("RSA");
            fl.read ( key, 0, fl.available() );
            fl.close();
            PKCS8EncodedKeySpec keysp = new PKCS8EncodedKeySpec ( key );
            PrivateKey ff = kf.generatePrivate (keysp);

            // loading CertificateChain
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            InputStream certstream = fullStream (certfile);

            Collection c = cf.generateCertificates(certstream);
            Certificate[] certs = new Certificate[c.toArray().length];

            if (c.size() == 1) {
                certstream = fullStream (certfile);
                System.out.println("One certificate, no chain.");
                Certificate cert = cf.generateCertificate(certstream) ;
                certs[0] = cert;
            } else {
                System.out.println("Certificate chain length: "+c.size());
                certs = (Certificate[])c.toArray();
            }

            // storing keystore
            ks.setKeyEntry(alias, ff, 
                           keypass.toCharArray(),
                           certs );
            System.out.println ("Key and certificate stored.");
            System.out.println ("Alias:"+alias+"  Password:"+keypass);
            ks.store(new FileOutputStream ( keystorename ),
                     keypass.toCharArray());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
