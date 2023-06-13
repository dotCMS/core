package com.dotcms.enterprise.license.bouncycastle.asn1.util;

import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1InputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Dump
{
    public static void main(String args[]) throws Exception {
        try(InputStream is = Files.newInputStream(Paths.get(args[0]));
                ASN1InputStream bIn = new ASN1InputStream(is)
        ){
            Object          obj;

            while ((obj = bIn.readObject()) != null)
            {
                System.out.println(ASN1Dump.dumpAsString(obj));
            }
        }
    }
}
