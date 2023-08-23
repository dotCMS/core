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

package com.dotcms.enterprise.license.bouncycastle.asn1.nist;

import com.dotcms.enterprise.license.bouncycastle.asn1.DERObjectIdentifier;
import com.dotcms.enterprise.license.bouncycastle.asn1.sec.SECNamedCurves;
import com.dotcms.enterprise.license.bouncycastle.asn1.sec.SECObjectIdentifiers;
import com.dotcms.enterprise.license.bouncycastle.asn1.x9.X9ECParameters;
import com.dotcms.enterprise.license.bouncycastle.util.Strings;

import java.util.Enumeration;
import java.util.Hashtable;

/**
 * Utility class for fetching curves using their NIST names as published in FIPS-PUB 186-2
 */
public class NISTNamedCurves
{
    static final Hashtable objIds = new Hashtable();
    static final Hashtable names = new Hashtable();

    static void defineCurve(String name, DERObjectIdentifier oid)
    {
        objIds.put(name, oid);
        names.put(oid, name);
    }

    static
    {
        // TODO Missing the "K-" curves

        defineCurve("B-571", SECObjectIdentifiers.sect571r1);
        defineCurve("B-409", SECObjectIdentifiers.sect409r1);
        defineCurve("B-283", SECObjectIdentifiers.sect283r1);
        defineCurve("B-233", SECObjectIdentifiers.sect233r1);
        defineCurve("B-163", SECObjectIdentifiers.sect163r2);
        defineCurve("P-521", SECObjectIdentifiers.secp521r1);
        defineCurve("P-384", SECObjectIdentifiers.secp384r1);
        defineCurve("P-256", SECObjectIdentifiers.secp256r1);
        defineCurve("P-224", SECObjectIdentifiers.secp224r1);
        defineCurve("P-192", SECObjectIdentifiers.secp192r1);
    }

    public static X9ECParameters getByName(
        String  name)
    {
        DERObjectIdentifier oid = (DERObjectIdentifier)objIds.get(Strings.toUpperCase(name));

        if (oid != null)
        {
            return getByOID(oid);
        }

        return null;
    }

    /**
     * return the X9ECParameters object for the named curve represented by
     * the passed in object identifier. Null if the curve isn't present.
     *
     * @param oid an object identifier representing a named curve, if present.
     */
    public static X9ECParameters getByOID(
        DERObjectIdentifier  oid)
    {
        return SECNamedCurves.getByOID(oid);
    }

    /**
     * return the object identifier signified by the passed in name. Null
     * if there is no object identifier associated with name.
     *
     * @return the object identifier associated with name, if present.
     */
    public static DERObjectIdentifier getOID(
        String  name)
    {
        return (DERObjectIdentifier)objIds.get(Strings.toUpperCase(name));
    }

    /**
     * return the named curve name represented by the given object identifier.
     */
    public static String getName(
        DERObjectIdentifier  oid)
    {
        return (String)names.get(oid);
    }

    /**
     * returns an enumeration containing the name strings for curves
     * contained in this structure.
     */
    public static Enumeration getNames()
    {
        return objIds.keys();
    }
}
