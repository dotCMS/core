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

package com.dotcms.enterprise.license.bouncycastle.crypto.engines;

import java.security.SecureRandom;

import com.dotcms.enterprise.license.bouncycastle.crypto.CipherParameters;
import com.dotcms.enterprise.license.bouncycastle.crypto.Digest;
import com.dotcms.enterprise.license.bouncycastle.crypto.InvalidCipherTextException;
import com.dotcms.enterprise.license.bouncycastle.crypto.Wrapper;
import com.dotcms.enterprise.license.bouncycastle.crypto.digests.SHA1Digest;
import com.dotcms.enterprise.license.bouncycastle.crypto.modes.CBCBlockCipher;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.KeyParameter;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.ParametersWithIV;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.ParametersWithRandom;
import com.dotcms.enterprise.license.bouncycastle.util.Arrays;

/**
 * Wrap keys according to
 * <A HREF="http://www.ietf.org/internet-drafts/draft-ietf-smime-key-wrap-01.txt">
 * draft-ietf-smime-key-wrap-01.txt</A>.
 * <p>
 * Note: 
 * <ul>
 * <li>this is based on a draft, and as such is subject to change - don't use this class for anything requiring long term storage.
 * <li>if you are using this to wrap triple-des keys you need to set the
 * parity bits on the key and, if it's a two-key triple-des key, pad it
 * yourself.
 * </ul>
 */
public class DESedeWrapEngine
    implements Wrapper
{
   /** Field engine */
   private CBCBlockCipher engine;

   /** Field param */
   private KeyParameter param;

   /** Field paramPlusIV */
   private ParametersWithIV paramPlusIV;

   /** Field iv */
   private byte[] iv;

   /** Field forWrapping */
   private boolean forWrapping;

   /** Field IV2           */
   private static final byte[] IV2 = { (byte) 0x4a, (byte) 0xdd, (byte) 0xa2,
                                       (byte) 0x2c, (byte) 0x79, (byte) 0xe8,
                                       (byte) 0x21, (byte) 0x05 };

    //
    // checksum digest
    //
    Digest  sha1 = new SHA1Digest();
    byte[]  digest = new byte[20];

   /**
    * Method init
    *
    * @param forWrapping
    * @param param
    */
    public void init(boolean forWrapping, CipherParameters param)
    {

        this.forWrapping = forWrapping;
        this.engine = new CBCBlockCipher(new DESedeEngine());

        SecureRandom sr;
        if (param instanceof ParametersWithRandom)
        {
            ParametersWithRandom pr = (ParametersWithRandom) param;
            param = pr.getParameters();
            sr = pr.getRandom();
        }
        else
        {
            sr = new SecureRandom();
        }

        if (param instanceof KeyParameter)
        {
            this.param = (KeyParameter)param;

            if (this.forWrapping)
            {

                // Hm, we have no IV but we want to wrap ?!?
                // well, then we have to create our own IV.
                this.iv = new byte[8];
                sr.nextBytes(iv);

                this.paramPlusIV = new ParametersWithIV(this.param, this.iv);
            }
        }
        else if (param instanceof ParametersWithIV)
        {
            this.paramPlusIV = (ParametersWithIV)param;
            this.iv = this.paramPlusIV.getIV();
            this.param = (KeyParameter)this.paramPlusIV.getParameters();

            if (this.forWrapping)
            {
                if ((this.iv == null) || (this.iv.length != 8))
                {
                    throw new IllegalArgumentException("IV is not 8 octets");
                }
            }
            else
            {
                throw new IllegalArgumentException(
                        "You should not supply an IV for unwrapping");
            }
        }
    }

   /**
    * Method getAlgorithmName
    *
    * @return the algorithm name "DESede".
    */
   public String getAlgorithmName() 
   {
      return "DESede";
   }

   /**
    * Method wrap
    *
    * @param in
    * @param inOff
    * @param inLen
    * @return the wrapped bytes.
    */
   public byte[] wrap(byte[] in, int inOff, int inLen) 
   {
      if (!forWrapping) 
      {
         throw new IllegalStateException("Not initialized for wrapping");
      }

      byte keyToBeWrapped[] = new byte[inLen];

      System.arraycopy(in, inOff, keyToBeWrapped, 0, inLen);

      // Compute the CMS Key Checksum, (section 5.6.1), call this CKS.
      byte[] CKS = calculateCMSKeyChecksum(keyToBeWrapped);

      // Let WKCKS = WK || CKS where || is concatenation.
      byte[] WKCKS = new byte[keyToBeWrapped.length + CKS.length];

      System.arraycopy(keyToBeWrapped, 0, WKCKS, 0, keyToBeWrapped.length);
      System.arraycopy(CKS, 0, WKCKS, keyToBeWrapped.length, CKS.length);

      // Encrypt WKCKS in CBC mode using KEK as the key and IV as the
      // initialization vector. Call the results TEMP1.

      int blockSize = engine.getBlockSize();

      if (WKCKS.length % blockSize != 0) 
      {
         throw new IllegalStateException("Not multiple of block length");
      }

      engine.init(true, paramPlusIV);

      byte TEMP1[] = new byte[WKCKS.length];

      for (int currentBytePos = 0; currentBytePos != WKCKS.length; currentBytePos += blockSize) 
      {
         engine.processBlock(WKCKS, currentBytePos, TEMP1, currentBytePos);
      }

      // Let TEMP2 = IV || TEMP1.
      byte[] TEMP2 = new byte[this.iv.length + TEMP1.length];

      System.arraycopy(this.iv, 0, TEMP2, 0, this.iv.length);
      System.arraycopy(TEMP1, 0, TEMP2, this.iv.length, TEMP1.length);

      // Reverse the order of the octets in TEMP2 and call the result TEMP3.
      byte[] TEMP3 = reverse(TEMP2);

      // Encrypt TEMP3 in CBC mode using the KEK and an initialization vector
      // of 0x 4a dd a2 2c 79 e8 21 05. The resulting cipher text is the desired
      // result. It is 40 octets long if a 168 bit key is being wrapped.
      ParametersWithIV param2 = new ParametersWithIV(this.param, IV2);

      this.engine.init(true, param2);

      for (int currentBytePos = 0; currentBytePos != TEMP3.length; currentBytePos += blockSize) 
      {
         engine.processBlock(TEMP3, currentBytePos, TEMP3, currentBytePos);
      }

      return TEMP3;
   }

   /**
    * Method unwrap
    *
    * @param in
    * @param inOff
    * @param inLen
    * @return the unwrapped bytes.
    * @throws InvalidCipherTextException
    */
    public byte[] unwrap(byte[] in, int inOff, int inLen)
           throws InvalidCipherTextException 
    {
        if (forWrapping)
        {
            throw new IllegalStateException("Not set for unwrapping");
        }
        
        if (in == null)
        {
            throw new InvalidCipherTextException("Null pointer as ciphertext");
        }

        final int blockSize = engine.getBlockSize();
        if (inLen % blockSize != 0)
        {
            throw new InvalidCipherTextException("Ciphertext not multiple of " + blockSize);
        }

      /*
      // Check if the length of the cipher text is reasonable given the key
      // type. It must be 40 bytes for a 168 bit key and either 32, 40, or
      // 48 bytes for a 128, 192, or 256 bit key. If the length is not supported
      // or inconsistent with the algorithm for which the key is intended,
      // return error.
      //
      // we do not accept 168 bit keys. it has to be 192 bit.
      int lengthA = (estimatedKeyLengthInBit / 8) + 16;
      int lengthB = estimatedKeyLengthInBit % 8;

      if ((lengthA != keyToBeUnwrapped.length) || (lengthB != 0)) {
         throw new XMLSecurityException("empty");
      }
      */

      // Decrypt the cipher text with TRIPLedeS in CBC mode using the KEK
      // and an initialization vector (IV) of 0x4adda22c79e82105. Call the output TEMP3.
      ParametersWithIV param2 = new ParametersWithIV(this.param, IV2);

      this.engine.init(false, param2);

      byte TEMP3[] = new byte[inLen];

      for (int currentBytePos = 0; currentBytePos != inLen; currentBytePos += blockSize) 
      {
         engine.processBlock(in, inOff + currentBytePos, TEMP3, currentBytePos);
      }

      // Reverse the order of the octets in TEMP3 and call the result TEMP2.
      byte[] TEMP2 = reverse(TEMP3);

      // Decompose TEMP2 into IV, the first 8 octets, and TEMP1, the remaining octets.
      this.iv = new byte[8];

      byte[] TEMP1 = new byte[TEMP2.length - 8];

      System.arraycopy(TEMP2, 0, this.iv, 0, 8);
      System.arraycopy(TEMP2, 8, TEMP1, 0, TEMP2.length - 8);

      // Decrypt TEMP1 using TRIPLedeS in CBC mode using the KEK and the IV
      // found in the previous step. Call the result WKCKS.
      this.paramPlusIV = new ParametersWithIV(this.param, this.iv);

      this.engine.init(false, this.paramPlusIV);

      byte[] WKCKS = new byte[TEMP1.length];

      for (int currentBytePos = 0; currentBytePos != WKCKS.length; currentBytePos += blockSize) 
      {
         engine.processBlock(TEMP1, currentBytePos, WKCKS, currentBytePos);
      }

      // Decompose WKCKS. CKS is the last 8 octets and WK, the wrapped key, are
      // those octets before the CKS.
      byte[] result = new byte[WKCKS.length - 8];
      byte[] CKStoBeVerified = new byte[8];

      System.arraycopy(WKCKS, 0, result, 0, WKCKS.length - 8);
      System.arraycopy(WKCKS, WKCKS.length - 8, CKStoBeVerified, 0, 8);

      // Calculate a CMS Key Checksum, (section 5.6.1), over the WK and compare
      // with the CKS extracted in the above step. If they are not equal, return error.
      if (!checkCMSKeyChecksum(result, CKStoBeVerified)) 
      {
         throw new InvalidCipherTextException(
            "Checksum inside ciphertext is corrupted");
      }

      // WK is the wrapped key, now extracted for use in data decryption.
      return result;
   }

    /**
     * Some key wrap algorithms make use of the Key Checksum defined
     * in CMS [CMS-Algorithms]. This is used to provide an integrity
     * check value for the key being wrapped. The algorithm is
     *
     * - Compute the 20 octet SHA-1 hash on the key being wrapped.
     * - Use the first 8 octets of this hash as the checksum value.
     *
     * @param key
     * @return the CMS checksum.
     * @throws RuntimeException
     * @see http://www.w3.org/TR/xmlenc-core/#sec-CMSKeyChecksum
     */
    private byte[] calculateCMSKeyChecksum(
        byte[] key)
    {
        byte[]  result = new byte[8];

        sha1.update(key, 0, key.length);
        sha1.doFinal(digest, 0);

        System.arraycopy(digest, 0, result, 0, 8);

        return result;
    }

    /**
     * @param key
     * @param checksum
     * @return true if okay, false otherwise.
     * @see http://www.w3.org/TR/xmlenc-core/#sec-CMSKeyChecksum
     */
    private boolean checkCMSKeyChecksum(
        byte[] key,
        byte[] checksum)
    {
        return Arrays.constantTimeAreEqual(calculateCMSKeyChecksum(key), checksum);
    }

    private static byte[] reverse(byte[] bs)
    {
        byte[] result = new byte[bs.length];
        for (int i = 0; i < bs.length; i++) 
        {
           result[i] = bs[bs.length - (i + 1)];
        }
        return result;
    }
}
