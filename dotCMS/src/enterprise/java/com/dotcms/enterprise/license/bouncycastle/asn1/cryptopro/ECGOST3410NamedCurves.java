/* 
* Licensed to dotCMS LLC under the dotCMS Enterprise License (the
* “Enterprise License”) found below 
* 
* Copyright (c) 2023 dotCMS Inc.
* 
* With regard to the dotCMS Software and this code:
* 
* This software, source code and associated documentation files (the
* "Software")  may only be modified and used if you (and any entity that
* you represent) have:
* 
* 1. Agreed to and are in compliance with, the dotCMS Subscription Terms
* of Service, available at https://www.dotcms.com/terms (the “Enterprise
* Terms”) or have another agreement governing the licensing and use of the
* Software between you and dotCMS. 2. Each dotCMS instance that uses
* enterprise features enabled by the code in this directory is licensed
* under these agreements and has a separate and valid dotCMS Enterprise
* server key issued by dotCMS.
* 
* Subject to these terms, you are free to modify this Software and publish
* patches to the Software if you agree that dotCMS and/or its licensors
* (as applicable) retain all right, title and interest in and to all such
* modifications and/or patches, and all such modifications and/or patches
* may only be used, copied, modified, displayed, distributed, or otherwise
* exploited with a valid dotCMS Enterprise license for the correct number
* of dotCMS instances.  You agree that dotCMS and/or its licensors (as
* applicable) retain all right, title and interest in and to all such
* modifications.  You are not granted any other rights beyond what is
* expressly stated herein.  Subject to the foregoing, it is forbidden to
* copy, merge, publish, distribute, sublicense, and/or sell the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
* IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
* CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
* TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
* SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
* 
* For all third party components incorporated into the dotCMS Software,
* those components are licensed under the original license provided by the
* owner of the applicable component.
*/

package com.dotcms.enterprise.license.bouncycastle.asn1.cryptopro;

import java.math.BigInteger;
import java.util.Enumeration;
import java.util.Hashtable;

import com.dotcms.enterprise.license.bouncycastle.asn1.DERObjectIdentifier;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.ECDomainParameters;
import com.dotcms.enterprise.license.bouncycastle.math.ec.ECCurve;
import com.dotcms.enterprise.license.bouncycastle.math.ec.ECFieldElement;
import com.dotcms.enterprise.license.bouncycastle.math.ec.ECPoint;

/**
 * table of the available named parameters for GOST 3410-2001.
 */
public class ECGOST3410NamedCurves
{
    static final Hashtable objIds = new Hashtable();
    static final Hashtable params = new Hashtable();
    static final Hashtable names = new Hashtable();

    static
    {
        BigInteger mod_p = new BigInteger("115792089237316195423570985008687907853269984665640564039457584007913129639319");
        BigInteger mod_q = new BigInteger("115792089237316195423570985008687907853073762908499243225378155805079068850323");
        
        ECCurve.Fp curve = new ECCurve.Fp(
            mod_p, // p
            new BigInteger("115792089237316195423570985008687907853269984665640564039457584007913129639316"), // a
            new BigInteger("166")); // b

        ECDomainParameters ecParams = new ECDomainParameters(
            curve,
            new ECPoint.Fp(curve,
                    new ECFieldElement.Fp(curve.getQ(),new BigInteger("1")), // x
                    new ECFieldElement.Fp(curve.getQ(),new BigInteger("64033881142927202683649881450433473985931760268884941288852745803908878638612"))), // y
            mod_q);
        
        params.put(CryptoProObjectIdentifiers.gostR3410_2001_CryptoPro_A, ecParams);  
        
        mod_p = new BigInteger("115792089237316195423570985008687907853269984665640564039457584007913129639319");
        mod_q = new BigInteger("115792089237316195423570985008687907853073762908499243225378155805079068850323");
        
        curve = new ECCurve.Fp(
                mod_p, // p
                new BigInteger("115792089237316195423570985008687907853269984665640564039457584007913129639316"),
                new BigInteger("166"));

        ecParams = new ECDomainParameters(
                curve,
                new ECPoint.Fp(curve,
                        new ECFieldElement.Fp(curve.getQ(),new BigInteger("1")), // x
                        new ECFieldElement.Fp(curve.getQ(),new BigInteger("64033881142927202683649881450433473985931760268884941288852745803908878638612"))), // y
                mod_q);

        params.put(CryptoProObjectIdentifiers.gostR3410_2001_CryptoPro_XchA, ecParams); 
        
        mod_p = new BigInteger("57896044618658097711785492504343953926634992332820282019728792003956564823193"); //p
        mod_q = new BigInteger("57896044618658097711785492504343953927102133160255826820068844496087732066703"); //q
        
        curve = new ECCurve.Fp(
            mod_p, // p
            new BigInteger("57896044618658097711785492504343953926634992332820282019728792003956564823190"), // a
            new BigInteger("28091019353058090096996979000309560759124368558014865957655842872397301267595")); // b

        ecParams = new ECDomainParameters(
            curve,
            new ECPoint.Fp(curve,
                           new ECFieldElement.Fp(mod_p,new BigInteger("1")), // x
                           new ECFieldElement.Fp(mod_p,new BigInteger("28792665814854611296992347458380284135028636778229113005756334730996303888124"))), // y
            mod_q); // q

        params.put(CryptoProObjectIdentifiers.gostR3410_2001_CryptoPro_B, ecParams);  
        
        mod_p = new BigInteger("70390085352083305199547718019018437841079516630045180471284346843705633502619");
        mod_q = new BigInteger("70390085352083305199547718019018437840920882647164081035322601458352298396601");
        
        curve = new ECCurve.Fp(
                mod_p, // p
                new BigInteger("70390085352083305199547718019018437841079516630045180471284346843705633502616"),
                new BigInteger("32858"));

        ecParams = new ECDomainParameters(
                curve,
                new ECPoint.Fp(curve,
                               new ECFieldElement.Fp(mod_p,new BigInteger("0")),
                               new ECFieldElement.Fp(mod_p,new BigInteger("29818893917731240733471273240314769927240550812383695689146495261604565990247"))),
            mod_q);
        
        params.put(CryptoProObjectIdentifiers.gostR3410_2001_CryptoPro_XchB, ecParams);  
                                
        mod_p = new BigInteger("70390085352083305199547718019018437841079516630045180471284346843705633502619"); //p
        mod_q = new BigInteger("70390085352083305199547718019018437840920882647164081035322601458352298396601"); //q
        curve = new ECCurve.Fp(
            mod_p, // p
            new BigInteger("70390085352083305199547718019018437841079516630045180471284346843705633502616"), // a
            new BigInteger("32858")); // b

        ecParams = new ECDomainParameters(
            curve,
            new ECPoint.Fp(curve,
                           new ECFieldElement.Fp(mod_p,new BigInteger("0")), // x
                           new ECFieldElement.Fp(mod_p,new BigInteger("29818893917731240733471273240314769927240550812383695689146495261604565990247"))), // y
            mod_q); // q

        params.put(CryptoProObjectIdentifiers.gostR3410_2001_CryptoPro_C, ecParams); 
            
        objIds.put("GostR3410-2001-CryptoPro-A", CryptoProObjectIdentifiers.gostR3410_2001_CryptoPro_A);
        objIds.put("GostR3410-2001-CryptoPro-B", CryptoProObjectIdentifiers.gostR3410_2001_CryptoPro_B);
        objIds.put("GostR3410-2001-CryptoPro-C", CryptoProObjectIdentifiers.gostR3410_2001_CryptoPro_C);
        objIds.put("GostR3410-2001-CryptoPro-XchA", CryptoProObjectIdentifiers.gostR3410_2001_CryptoPro_XchA);
        objIds.put("GostR3410-2001-CryptoPro-XchB", CryptoProObjectIdentifiers.gostR3410_2001_CryptoPro_XchB);
        
        names.put(CryptoProObjectIdentifiers.gostR3410_2001_CryptoPro_A, "GostR3410-2001-CryptoPro-A");
        names.put(CryptoProObjectIdentifiers.gostR3410_2001_CryptoPro_B, "GostR3410-2001-CryptoPro-B");
        names.put(CryptoProObjectIdentifiers.gostR3410_2001_CryptoPro_C, "GostR3410-2001-CryptoPro-C");
        names.put(CryptoProObjectIdentifiers.gostR3410_2001_CryptoPro_XchA, "GostR3410-2001-CryptoPro-XchA");
        names.put(CryptoProObjectIdentifiers.gostR3410_2001_CryptoPro_XchB, "GostR3410-2001-CryptoPro-XchB");
    }

    /**
     * return the ECDomainParameters object for the given OID, null if it 
     * isn't present.
     *
     * @param oid an object identifier representing a named parameters, if present.
     */
    public static ECDomainParameters getByOID(
        DERObjectIdentifier  oid)
    {
        return (ECDomainParameters)params.get(oid);
    }

    /**
     * returns an enumeration containing the name strings for parameters
     * contained in this structure.
     */
    public static Enumeration getNames()
    {
        return objIds.keys();
    }

    public static ECDomainParameters getByName(
        String  name)
    {
        DERObjectIdentifier oid = (DERObjectIdentifier)objIds.get(name);

        if (oid != null)
        {
            return (ECDomainParameters)params.get(oid);
        }

        return null;
    }

    /**
     * return the named curve name represented by the given object identifier.
     */
    public static String getName(
        DERObjectIdentifier  oid)
    {
        return (String)names.get(oid);
    }
    
    public static DERObjectIdentifier getOID(String name)
    {
        return (DERObjectIdentifier)objIds.get(name);
    }
}
