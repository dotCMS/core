package com.dotcms.cli.security;


import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Collection;

public class TrustStoreImportKey  {
    
    private static InputStream fullStream ( String fname ) throws IOException {
        FileInputStream fis = new FileInputStream(fname);
        DataInputStream dis = new DataInputStream(fis);
        byte[] bytes = new byte[dis.available()];
        dis.readFully(bytes);
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        return bais;
    }
    
    /**
     * This main takes 4 parameters:
     *	 	-	truststore (absolute path);
     *	      -	truststore password;
     *		-	alias;
     *		-	certfile;
    */    	    
    public static void main ( String args[]) {
        
	  if (args.length != 4) {
            System.out.println("Usage: java com.dotcms.cli.security.TrustStoreImportKey truststore truststore_password alias certfile ");
            System.exit(0);
        }
        String keypass = args[1];
        String alias = args[2];
        String truststorename = args[0];

        if (truststorename == null){
		System.out.println("Error: you must pass the truststorename file");
            System.exit(0);		
	  }

        String certfile = args[3];

        try {
            KeyStore ks = KeyStore.getInstance("JKS", "SUN");
            System.out.println("Using truststore-file : "+truststorename);
            ks.load(new FileInputStream ( truststorename ),
                    keypass.toCharArray());

            // loading CertificateChain
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            InputStream certstream = fullStream (certfile);

            Collection c = cf.generateCertificates(certstream) ;
            Certificate[] certs = new Certificate[c.toArray().length];

            if (c.size() == 1) {
                certstream = fullStream (certfile);
                System.out.println("One certificate, no chain.");
                Certificate cert = cf.generateCertificate(certstream) ;
                certs[0] = cert;
            } 

            ks.setCertificateEntry(alias, certs[0]);
            System.out.println ("Certificate stored.");
            System.out.println ("Alias:"+alias+"  Password:"+keypass);
            ks.store(new FileOutputStream ( truststorename ),
                     keypass.toCharArray());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
