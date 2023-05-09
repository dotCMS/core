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

package com.dotcms.enterprise.license.bouncycastle.crypto.tls;

import com.dotcms.enterprise.license.bouncycastle.crypto.digests.SHA1Digest;
import com.dotcms.enterprise.license.bouncycastle.crypto.engines.AESFastEngine;
import com.dotcms.enterprise.license.bouncycastle.crypto.engines.DESedeEngine;
import com.dotcms.enterprise.license.bouncycastle.crypto.modes.CBCBlockCipher;

import java.io.IOException;
import java.io.OutputStream;

/**
 * A manager for ciphersuite. This class does manage all ciphersuites
 * which are used by MicroTLS.
 */
public class TlsCipherSuiteManager
{
    private static final int TLS_RSA_WITH_3DES_EDE_CBC_SHA = 0x000a;
    private static final int TLS_DHE_DSS_WITH_3DES_EDE_CBC_SHA = 0x0013;
    private static final int TLS_DHE_RSA_WITH_3DES_EDE_CBC_SHA = 0x0016;
    private static final int TLS_RSA_WITH_AES_128_CBC_SHA = 0x002f;
    private static final int TLS_DHE_DSS_WITH_AES_128_CBC_SHA = 0x0032;
    private static final int TLS_DHE_RSA_WITH_AES_128_CBC_SHA = 0x0033;
    private static final int TLS_RSA_WITH_AES_256_CBC_SHA = 0x0035;
    private static final int TLS_DHE_DSS_WITH_AES_256_CBC_SHA = 0x0038;
    private static final int TLS_DHE_RSA_WITH_AES_256_CBC_SHA = 0x0039;

//    private static final int TLS_SRP_SHA_WITH_3DES_EDE_CBC_SHA = 0xC01A;    
//    private static final int TLS_SRP_SHA_RSA_WITH_3DES_EDE_CBC_SHA = 0xC01B;
//    private static final int TLS_SRP_SHA_DSS_WITH_3DES_EDE_CBC_SHA = 0xC01C;
//    private static final int TLS_SRP_SHA_WITH_AES_128_CBC_SHA = 0xC01D;
//    private static final int TLS_SRP_SHA_RSA_WITH_AES_128_CBC_SHA = 0xC01E;
//    private static final int TLS_SRP_SHA_DSS_WITH_AES_128_CBC_SHA = 0xC01F;
//    private static final int TLS_SRP_SHA_WITH_AES_256_CBC_SHA = 0xC020;
//    private static final int TLS_SRP_SHA_RSA_WITH_AES_256_CBC_SHA = 0xC021;
//    private static final int TLS_SRP_SHA_DSS_WITH_AES_256_CBC_SHA = 0xC022;

    protected static void writeCipherSuites(OutputStream os) throws IOException
    {
        int[] suites = new int[]
        {
            TLS_DHE_RSA_WITH_AES_256_CBC_SHA,
            TLS_DHE_DSS_WITH_AES_256_CBC_SHA,
            TLS_DHE_RSA_WITH_AES_128_CBC_SHA,
            TLS_DHE_DSS_WITH_AES_128_CBC_SHA,
            TLS_DHE_RSA_WITH_3DES_EDE_CBC_SHA,
            TLS_DHE_DSS_WITH_3DES_EDE_CBC_SHA,
            TLS_RSA_WITH_AES_256_CBC_SHA,
            TLS_RSA_WITH_AES_128_CBC_SHA,
            TLS_RSA_WITH_3DES_EDE_CBC_SHA,

//            TLS_SRP_SHA_DSS_WITH_AES_256_CBC_SHA,
//            TLS_SRP_SHA_DSS_WITH_AES_128_CBC_SHA,
//            TLS_SRP_SHA_DSS_WITH_3DES_EDE_CBC_SHA,
//            TLS_SRP_SHA_RSA_WITH_AES_256_CBC_SHA,
//            TLS_SRP_SHA_RSA_WITH_AES_128_CBC_SHA,
//            TLS_SRP_SHA_RSA_WITH_3DES_EDE_CBC_SHA,
//            TLS_SRP_SHA_WITH_AES_256_CBC_SHA,
//            TLS_SRP_SHA_WITH_AES_128_CBC_SHA,
//            TLS_SRP_SHA_WITH_3DES_EDE_CBC_SHA,
        };

       TlsUtils.writeUint16(2 * suites.length, os);
       for (int i = 0; i < suites.length; ++i)
       {
           TlsUtils.writeUint16(suites[i], os);
       }
    }

    protected static TlsCipherSuite getCipherSuite(int number, TlsProtocolHandler handler) throws IOException
    {
        switch (number)
        {
            case TLS_RSA_WITH_3DES_EDE_CBC_SHA:
                return createDESedeCipherSuite(24, TlsCipherSuite.KE_RSA);

            case TLS_DHE_DSS_WITH_3DES_EDE_CBC_SHA:
                return createDESedeCipherSuite(24, TlsCipherSuite.KE_DHE_DSS);

            case TLS_DHE_RSA_WITH_3DES_EDE_CBC_SHA:
                return createDESedeCipherSuite(24, TlsCipherSuite.KE_DHE_RSA);

            case TLS_RSA_WITH_AES_128_CBC_SHA:
                return createAESCipherSuite(16, TlsCipherSuite.KE_RSA);

            case TLS_DHE_DSS_WITH_AES_128_CBC_SHA:
                return createAESCipherSuite(16, TlsCipherSuite.KE_DHE_DSS);

            case TLS_DHE_RSA_WITH_AES_128_CBC_SHA:
                return createAESCipherSuite(16, TlsCipherSuite.KE_DHE_RSA);

            case TLS_RSA_WITH_AES_256_CBC_SHA:
                return createAESCipherSuite(32, TlsCipherSuite.KE_RSA);

            case TLS_DHE_DSS_WITH_AES_256_CBC_SHA:
                return createAESCipherSuite(32, TlsCipherSuite.KE_DHE_DSS);

            case TLS_DHE_RSA_WITH_AES_256_CBC_SHA:
                return createAESCipherSuite(32, TlsCipherSuite.KE_DHE_RSA);

//            case TLS_SRP_SHA_WITH_3DES_EDE_CBC_SHA:
//                return createDESedeCipherSuite(24, TlsCipherSuite.KE_SRP);
//
//            case TLS_SRP_SHA_RSA_WITH_3DES_EDE_CBC_SHA:
//                return createDESedeCipherSuite(24, TlsCipherSuite.KE_SRP_RSA);
//
//            case TLS_SRP_SHA_DSS_WITH_3DES_EDE_CBC_SHA:
//                return createDESedeCipherSuite(24, TlsCipherSuite.KE_SRP_DSS);
//
//            case TLS_SRP_SHA_WITH_AES_128_CBC_SHA:
//                return createAESCipherSuite(16, TlsCipherSuite.KE_SRP);
//
//            case TLS_SRP_SHA_RSA_WITH_AES_128_CBC_SHA:
//                return createAESCipherSuite(16, TlsCipherSuite.KE_SRP_RSA);
//
//            case TLS_SRP_SHA_DSS_WITH_AES_128_CBC_SHA:
//                return createAESCipherSuite(16, TlsCipherSuite.KE_SRP_DSS);
//
//            case TLS_SRP_SHA_WITH_AES_256_CBC_SHA:
//                return createAESCipherSuite(32, TlsCipherSuite.KE_SRP);
//
//            case TLS_SRP_SHA_RSA_WITH_AES_256_CBC_SHA:
//                return createAESCipherSuite(32, TlsCipherSuite.KE_SRP_RSA);
//
//            case TLS_SRP_SHA_DSS_WITH_AES_256_CBC_SHA:
//                return createAESCipherSuite(32, TlsCipherSuite.KE_SRP_DSS);

            default:
                handler.failWithError(TlsProtocolHandler.AL_fatal, TlsProtocolHandler.AP_handshake_failure);

                /*
                * Unreachable Code, failWithError will always throw an exception!
                */
                return null;
        }
    }

    private static TlsCipherSuite createAESCipherSuite(int cipherKeySize, short keyExchange)
    {
        return new TlsBlockCipherCipherSuite(createAESCipher(), createAESCipher(),
            new SHA1Digest(), new SHA1Digest(), cipherKeySize, keyExchange);
    }

    private static TlsCipherSuite createDESedeCipherSuite(int cipherKeySize, short keyExchange)
    {
        return new TlsBlockCipherCipherSuite(createDESedeCipher(), createDESedeCipher(),
            new SHA1Digest(), new SHA1Digest(), cipherKeySize, keyExchange);
    }

    private static CBCBlockCipher createAESCipher()
    {
        return new CBCBlockCipher(new AESFastEngine());
    }
    
    private static CBCBlockCipher createDESedeCipher()
    {
        return new CBCBlockCipher(new DESedeEngine());
    }
}
