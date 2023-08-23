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

package com.dotcms.enterprise.license.bouncycastle.asn1.misc;

import com.dotcms.enterprise.license.bouncycastle.asn1.DERObjectIdentifier;

public interface MiscObjectIdentifiers
{
    //
    // Netscape
    //       iso/itu(2) joint-assign(16) us(840) uscompany(1) netscape(113730) cert-extensions(1) }
    //
    static final String                 netscape                = "2.16.840.1.113730.1";
    static final DERObjectIdentifier    netscapeCertType        = new DERObjectIdentifier(netscape + ".1");
    static final DERObjectIdentifier    netscapeBaseURL         = new DERObjectIdentifier(netscape + ".2");
    static final DERObjectIdentifier    netscapeRevocationURL   = new DERObjectIdentifier(netscape + ".3");
    static final DERObjectIdentifier    netscapeCARevocationURL = new DERObjectIdentifier(netscape + ".4");
    static final DERObjectIdentifier    netscapeRenewalURL      = new DERObjectIdentifier(netscape + ".7");
    static final DERObjectIdentifier    netscapeCApolicyURL     = new DERObjectIdentifier(netscape + ".8");
    static final DERObjectIdentifier    netscapeSSLServerName   = new DERObjectIdentifier(netscape + ".12");
    static final DERObjectIdentifier    netscapeCertComment     = new DERObjectIdentifier(netscape + ".13");

    //
    // Novell
    //       iso/itu(2) country(16) us(840) organization(1) novell(113719)
    //
    static final String                 novell                  = "2.16.840.1.113719";
    static final DERObjectIdentifier    novellSecurityAttribs   = new DERObjectIdentifier(novell + ".1.9.4.1");

    //
    // Entrust
    //       iso(1) member-body(16) us(840) nortelnetworks(113533) entrust(7)
    //
    static final String                 entrust                 = "1.2.840.113533.7";
    static final DERObjectIdentifier    entrustVersionExtension = new DERObjectIdentifier(entrust + ".65.0");
}
