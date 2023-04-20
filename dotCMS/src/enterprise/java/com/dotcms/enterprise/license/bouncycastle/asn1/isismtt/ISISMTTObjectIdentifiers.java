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

package com.dotcms.enterprise.license.bouncycastle.asn1.isismtt;

import com.dotcms.enterprise.license.bouncycastle.asn1.DERObjectIdentifier;

public interface ISISMTTObjectIdentifiers
{

    public static final DERObjectIdentifier id_isismtt = new DERObjectIdentifier("1.3.36.8");

    public static final DERObjectIdentifier id_isismtt_cp = new DERObjectIdentifier(id_isismtt + ".1");

    /**
     * The id-isismtt-cp-accredited OID indicates that the certificate is a
     * qualified certificate according to Directive 1999/93/EC of the European
     * Parliament and of the Council of 13 December 1999 on a Community
     * Framework for Electronic Signatures, which additionally conforms the
     * special requirements of the SigG and has been issued by an accredited CA.
     */
    public static final DERObjectIdentifier id_isismtt_cp_accredited = new DERObjectIdentifier(id_isismtt_cp + ".1");

    public static final DERObjectIdentifier id_isismtt_at = new DERObjectIdentifier(id_isismtt + ".3");

    /**
     * Certificate extensionDate of certificate generation
     * 
     * <pre>
     *                DateOfCertGenSyntax ::= GeneralizedTime
     * </pre>
     */
    public static final DERObjectIdentifier id_isismtt_at_dateOfCertGen = new DERObjectIdentifier(id_isismtt_at + ".1");

    /**
     * Attribute to indicate that the certificate holder may sign in the name of
     * a third person. May also be used as extension in a certificate.
     */
    public static final DERObjectIdentifier id_isismtt_at_procuration = new DERObjectIdentifier(id_isismtt_at + ".2");

    /**
     * Attribute to indicate admissions to certain professions. May be used as
     * attribute in attribute certificate or as extension in a certificate
     */
    public static final DERObjectIdentifier id_isismtt_at_admission = new DERObjectIdentifier(id_isismtt_at + ".3");

    /**
     * Monetary limit for transactions. The QcEuMonetaryLimit QC statement MUST
     * be used in new certificates in place of the extension/attribute
     * MonetaryLimit since January 1, 2004. For the sake of backward
     * compatibility with certificates already in use, SigG conforming
     * components MUST support MonetaryLimit (as well as QcEuLimitValue).
     */
    public static final DERObjectIdentifier id_isismtt_at_monetaryLimit = new DERObjectIdentifier(id_isismtt_at + ".4");

    /**
     * A declaration of majority. May be used as attribute in attribute
     * certificate or as extension in a certificate
     */
    public static final DERObjectIdentifier id_isismtt_at_declarationOfMajority = new DERObjectIdentifier(id_isismtt_at + ".5");

    /**
     * 
     * Serial number of the smart card containing the corresponding private key
     * 
     * <pre>
     *                 ICCSNSyntax ::= OCTET STRING (SIZE(8..20))
     * </pre>
     */
    public static final DERObjectIdentifier id_isismtt_at_iCCSN = new DERObjectIdentifier(id_isismtt_at + ".6");

    /**
     * 
     * Reference for a file of a smartcard that stores the public key of this
     * certificate and that is used as �security anchor�.
     * 
     * <pre>
     *      PKReferenceSyntax ::= OCTET STRING (SIZE(20))
     * </pre>
     */
    public static final DERObjectIdentifier id_isismtt_at_PKReference = new DERObjectIdentifier(id_isismtt_at + ".7");

    /**
     * Some other restriction regarding the usage of this certificate. May be
     * used as attribute in attribute certificate or as extension in a
     * certificate.
     * 
     * <pre>
     *             RestrictionSyntax ::= DirectoryString (SIZE(1..1024))
     * </pre>
     * 
     * @see com.dotcms.enterprise.license.bouncycastle.asn1.isismtt.x509.Restriction
     */
    public static final DERObjectIdentifier id_isismtt_at_restriction = new DERObjectIdentifier(id_isismtt_at + ".8");

    /**
     * 
     * (Single)Request extension: Clients may include this extension in a
     * (single) Request to request the responder to send the certificate in the
     * response message along with the status information. Besides the LDAP
     * service, this extension provides another mechanism for the distribution
     * of certificates, which MAY optionally be provided by certificate
     * repositories.
     * 
     * <pre>
     *        RetrieveIfAllowed ::= BOOLEAN
     *       
     * </pre>
     */
    public static final DERObjectIdentifier id_isismtt_at_retrieveIfAllowed = new DERObjectIdentifier(id_isismtt_at + ".9");

    /**
     * SingleOCSPResponse extension: The certificate requested by the client by
     * inserting the RetrieveIfAllowed extension in the request, will be
     * returned in this extension.
     * 
     * @see com.dotcms.enterprise.license.bouncycastle.asn1.isismtt.ocsp.RequestedCertificate
     */
    public static final DERObjectIdentifier id_isismtt_at_requestedCertificate = new DERObjectIdentifier(id_isismtt_at + ".10");

    /**
     * Base ObjectIdentifier for naming authorities
     */
    public static final DERObjectIdentifier id_isismtt_at_namingAuthorities = new DERObjectIdentifier(id_isismtt_at + ".11");

    /**
     * SingleOCSPResponse extension: Date, when certificate has been published
     * in the directory and status information has become available. Currently,
     * accrediting authorities enforce that SigG-conforming OCSP servers include
     * this extension in the responses.
     * 
     * <pre>
     *      CertInDirSince ::= GeneralizedTime
     * </pre>
     */
    public static final DERObjectIdentifier id_isismtt_at_certInDirSince = new DERObjectIdentifier(id_isismtt_at + ".12");

    /**
     * Hash of a certificate in OCSP.
     * 
     * @see com.dotcms.enterprise.license.bouncycastle.asn1.isismtt.ocsp.CertHash
     */
    public static final DERObjectIdentifier id_isismtt_at_certHash = new DERObjectIdentifier(id_isismtt_at + ".13");

    /**
     * <pre>
     *          NameAtBirth ::= DirectoryString(SIZE(1..64)
     * </pre>
     * 
     * Used in
     * {@link com.dotcms.enterprise.license.bouncycastle.asn1.x509.SubjectDirectoryAttributes SubjectDirectoryAttributes}
     */
    public static final DERObjectIdentifier id_isismtt_at_nameAtBirth = new DERObjectIdentifier(id_isismtt_at + ".14");

    /**
     * Some other information of non-restrictive nature regarding the usage of
     * this certificate. May be used as attribute in atribute certificate or as
     * extension in a certificate.
     * 
     * <pre>
     *               AdditionalInformationSyntax ::= DirectoryString (SIZE(1..2048))
     * </pre>
     * 
     * @see com.dotcms.enterprise.license.bouncycastle.asn1.isismtt.x509.AdditionalInformationSyntax
     */
    public static final DERObjectIdentifier id_isismtt_at_additionalInformation = new DERObjectIdentifier(id_isismtt_at + ".15");

    /**
     * Indicates that an attribute certificate exists, which limits the
     * usability of this public key certificate. Whenever verifying a signature
     * with the help of this certificate, the content of the corresponding
     * attribute certificate should be concerned. This extension MUST be
     * included in a PKC, if a corresponding attribute certificate (having the
     * PKC as base certificate) contains some attribute that restricts the
     * usability of the PKC too. Attribute certificates with restricting content
     * MUST always be included in the signed document.
     * 
     * <pre>
     *                   LiabilityLimitationFlagSyntax ::= BOOLEAN
     * </pre>
     */
    public static final DERObjectIdentifier id_isismtt_at_liabilityLimitationFlag = new DERObjectIdentifier("0.2.262.1.10.12.0");
}
