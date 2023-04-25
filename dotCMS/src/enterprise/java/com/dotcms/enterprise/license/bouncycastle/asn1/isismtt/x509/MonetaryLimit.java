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

package com.dotcms.enterprise.license.bouncycastle.asn1.isismtt.x509;

import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Encodable;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1EncodableVector;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Sequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERInteger;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERPrintableString;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERSequence;

import java.math.BigInteger;
import java.util.Enumeration;

/**
 * Monetary limit for transactions. The QcEuMonetaryLimit QC statement MUST be
 * used in new certificates in place of the extension/attribute MonetaryLimit
 * since January 1, 2004. For the sake of backward compatibility with
 * certificates already in use, components SHOULD support MonetaryLimit (as well
 * as QcEuLimitValue).
 * <p/>
 * Indicates a monetary limit within which the certificate holder is authorized
 * to act. (This value DOES NOT express a limit on the liability of the
 * certification authority).
 * <p/>
 * <pre>
 *    MonetaryLimitSyntax ::= SEQUENCE
 *    {
 *      currency PrintableString (SIZE(3)),
 *      amount INTEGER,
 *      exponent INTEGER
 *    }
 * </pre>
 * <p/>
 * currency must be the ISO code.
 * <p/>
 * value = amount�10*exponent
 */
public class MonetaryLimit
    extends ASN1Encodable
{
    DERPrintableString currency;
    DERInteger amount;
    DERInteger exponent;

    public static MonetaryLimit getInstance(Object obj)
    {
        if (obj == null || obj instanceof MonetaryLimit)
        {
            return (MonetaryLimit)obj;
        }

        if (obj instanceof ASN1Sequence)
        {
            return new MonetaryLimit(ASN1Sequence.getInstance(obj));
        }

        throw new IllegalArgumentException("unknown object in getInstance");
    }

    private MonetaryLimit(ASN1Sequence seq)
    {
        if (seq.size() != 3)
        {
            throw new IllegalArgumentException("Bad sequence size: "
                + seq.size());
        }
        Enumeration e = seq.getObjects();
        currency = DERPrintableString.getInstance(e.nextElement());
        amount = DERInteger.getInstance(e.nextElement());
        exponent = DERInteger.getInstance(e.nextElement());
    }

    /**
     * Constructor from a given details.
     * <p/>
     * <p/>
     * value = amount�10^exponent
     *
     * @param currency The currency. Must be the ISO code.
     * @param amount   The amount
     * @param exponent The exponent
     */
    public MonetaryLimit(String currency, int amount, int exponent)
    {
        this.currency = new DERPrintableString(currency, true);
        this.amount = new DERInteger(amount);
        this.exponent = new DERInteger(exponent);
    }

    public String getCurrency()
    {
        return currency.getString();
    }

    public BigInteger getAmount()
    {
        return amount.getValue();
    }

    public BigInteger getExponent()
    {
        return exponent.getValue();
    }

    /**
     * Produce an object suitable for an ASN1OutputStream.
     * <p/>
     * Returns:
     * <p/>
     * <pre>
     *    MonetaryLimitSyntax ::= SEQUENCE
     *    {
     *      currency PrintableString (SIZE(3)),
     *      amount INTEGER,
     *      exponent INTEGER
     *    }
     * </pre>
     *
     * @return a DERObject
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector seq = new ASN1EncodableVector();
        seq.add(currency);
        seq.add(amount);
        seq.add(exponent);

        return new DERSequence(seq);
    }

}
