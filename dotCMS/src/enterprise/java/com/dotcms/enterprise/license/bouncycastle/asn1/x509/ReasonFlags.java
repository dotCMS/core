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

package com.dotcms.enterprise.license.bouncycastle.asn1.x509;

import com.dotcms.enterprise.license.bouncycastle.asn1.DERBitString;

/**
 * The ReasonFlags object.
 * <pre>
 * ReasonFlags ::= BIT STRING {
 *      unused                  (0),
 *      keyCompromise           (1),
 *      cACompromise            (2),
 *      affiliationChanged      (3),
 *      superseded              (4),
 *      cessationOfOperation    (5),
 *      certificateHold         (6),
 *      privilegeWithdrawn      (7),
 *      aACompromise            (8) }
 * </pre>
 */
public class ReasonFlags
    extends DERBitString
{
    /**
     * @deprecated use lower case version
     */
    public static final int UNUSED                  = (1 << 7);
    /**
     * @deprecated use lower case version
     */
    public static final int KEY_COMPROMISE          = (1 << 6);
    /**
     * @deprecated use lower case version
     */
    public static final int CA_COMPROMISE           = (1 << 5);
    /**
     * @deprecated use lower case version
     */
    public static final int AFFILIATION_CHANGED     = (1 << 4);
    /**
     * @deprecated use lower case version
     */
    public static final int SUPERSEDED              = (1 << 3);
    /**
     * @deprecated use lower case version
     */
    public static final int CESSATION_OF_OPERATION  = (1 << 2);
    /**
     * @deprecated use lower case version
     */
    public static final int CERTIFICATE_HOLD        = (1 << 1);
    /**
     * @deprecated use lower case version
     */
    public static final int PRIVILEGE_WITHDRAWN     = (1 << 0);
    /**
     * @deprecated use lower case version
     */
    public static final int AA_COMPROMISE           = (1 << 15);
    
    public static final int unused                  = (1 << 7);
    public static final int keyCompromise           = (1 << 6);
    public static final int cACompromise            = (1 << 5);
    public static final int affiliationChanged      = (1 << 4);
    public static final int superseded              = (1 << 3);
    public static final int cessationOfOperation    = (1 << 2);
    public static final int certificateHold         = (1 << 1);
    public static final int privilegeWithdrawn      = (1 << 0);
    public static final int aACompromise            = (1 << 15);

    /**
     * @param reasons - the bitwise OR of the Key Reason flags giving the
     * allowed uses for the key.
     */
    public ReasonFlags(
        int reasons)
    {
        super(getBytes(reasons), getPadBits(reasons));
    }

    public ReasonFlags(
        DERBitString reasons)
    {
        super(reasons.getBytes(), reasons.getPadBits());
    }
}
