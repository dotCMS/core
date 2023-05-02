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

import com.dotcms.enterprise.license.bouncycastle.asn1.DEREnumerated;

/**
 * The CRLReason enumeration.
 * <pre>
 * CRLReason ::= ENUMERATED {
 *  unspecified             (0),
 *  keyCompromise           (1),
 *  cACompromise            (2),
 *  affiliationChanged      (3),
 *  superseded              (4),
 *  cessationOfOperation    (5),
 *  certificateHold         (6),
 *  removeFromCRL           (8),
 *  privilegeWithdrawn      (9),
 *  aACompromise           (10)
 * }
 * </pre>
 */
public class CRLReason
    extends DEREnumerated
{
    /**
     * @deprecated use lower case version
     */
    public static final int UNSPECIFIED = 0;
    /**
     * @deprecated use lower case version
     */
    public static final int KEY_COMPROMISE = 1;
    /**
     * @deprecated use lower case version
     */
    public static final int CA_COMPROMISE = 2;
    /**
     * @deprecated use lower case version
     */
    public static final int AFFILIATION_CHANGED = 3;
    /**
     * @deprecated use lower case version
     */
    public static final int SUPERSEDED = 4;
    /**
     * @deprecated use lower case version
     */
    public static final int CESSATION_OF_OPERATION  = 5;
    /**
     * @deprecated use lower case version
     */
    public static final int CERTIFICATE_HOLD = 6;
    /**
     * @deprecated use lower case version
     */
    public static final int REMOVE_FROM_CRL = 8;
    /**
     * @deprecated use lower case version
     */
    public static final int PRIVILEGE_WITHDRAWN = 9;
    /**
     * @deprecated use lower case version
     */
    public static final int AA_COMPROMISE = 10;

    public static final int unspecified = 0;
    public static final int keyCompromise = 1;
    public static final int cACompromise = 2;
    public static final int affiliationChanged = 3;
    public static final int superseded = 4;
    public static final int cessationOfOperation  = 5;
    public static final int certificateHold = 6;
    // 7 -> unknown
    public static final int removeFromCRL = 8;
    public static final int privilegeWithdrawn = 9;
    public static final int aACompromise = 10;

    private static final String[] reasonString =
    {
        "unspecified", "keyCompromise", "cACompromise", "affiliationChanged",
        "superseded", "cessationOfOperation", "certificateHold", "unknown",
        "removeFromCRL", "privilegeWithdrawn", "aACompromise"
    };

    public CRLReason(
        int reason)
    {
        super(reason);
    }

    public CRLReason(
        DEREnumerated reason)
    {
        super(reason.getValue().intValue());
    }

    public String toString()
    {
        String str;
        int reason = getValue().intValue();
        if (reason < 0 || reason > 10)
        {
            str = "invalid";
        }
        else
        {
            str = reasonString[reason];
        }
        return "CRLReason: " + str;
    }    
}
