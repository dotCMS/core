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

package com.dotcms.enterprise.license.bouncycastle.asn1.oiw;

import com.dotcms.enterprise.license.bouncycastle.asn1.DERObjectIdentifier;

public interface OIWObjectIdentifiers
{
    // id-SHA1 OBJECT IDENTIFIER ::=    
    //   {iso(1) identified-organization(3) oiw(14) secsig(3) algorithms(2) 26 }    //
    static final DERObjectIdentifier    md4WithRSA              = new DERObjectIdentifier("1.3.14.3.2.2");
    static final DERObjectIdentifier    md5WithRSA              = new DERObjectIdentifier("1.3.14.3.2.3");
    static final DERObjectIdentifier    md4WithRSAEncryption    = new DERObjectIdentifier("1.3.14.3.2.4");
    
    static final DERObjectIdentifier    desECB                  = new DERObjectIdentifier("1.3.14.3.2.6");
    static final DERObjectIdentifier    desCBC                  = new DERObjectIdentifier("1.3.14.3.2.7");
    static final DERObjectIdentifier    desOFB                  = new DERObjectIdentifier("1.3.14.3.2.8");
    static final DERObjectIdentifier    desCFB                  = new DERObjectIdentifier("1.3.14.3.2.9");

    static final DERObjectIdentifier    desEDE                  = new DERObjectIdentifier("1.3.14.3.2.17");
    
    static final DERObjectIdentifier    idSHA1                  = new DERObjectIdentifier("1.3.14.3.2.26");

    static final DERObjectIdentifier    dsaWithSHA1             = new DERObjectIdentifier("1.3.14.3.2.27");

    static final DERObjectIdentifier    sha1WithRSA             = new DERObjectIdentifier("1.3.14.3.2.29");
    
    // ElGamal Algorithm OBJECT IDENTIFIER ::=    
    // {iso(1) identified-organization(3) oiw(14) dirservsig(7) algorithm(2) encryption(1) 1 }
    //
    static final DERObjectIdentifier    elGamalAlgorithm        = new DERObjectIdentifier("1.3.14.7.2.1.1");

}
