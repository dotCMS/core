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

package com.dotcms.enterprise.license.bouncycastle.crypto.modes;

import com.dotcms.enterprise.license.bouncycastle.crypto.BlockCipher;
import com.dotcms.enterprise.license.bouncycastle.crypto.CipherParameters;
import com.dotcms.enterprise.license.bouncycastle.crypto.DataLengthException;
import com.dotcms.enterprise.license.bouncycastle.crypto.InvalidCipherTextException;

/**
 * A block cipher mode that includes authenticated encryption with a streaming mode and optional associated data.
 * @see com.dotcms.enterprise.license.bouncycastle.crypto.params.AEADParameters
 */
public interface AEADBlockCipher
{
    /**
     * initialise the underlying cipher. Parameter can either be an AEADParameters or a ParametersWithIV object.
     *
     * @param forEncryption true if we are setting up for encryption, false otherwise.
     * @param params the necessary parameters for the underlying cipher to be initialised.
     * @exception IllegalArgumentException if the params argument is inappropriate.
     */
    public void init(boolean forEncryption, CipherParameters params)
        throws IllegalArgumentException;

    /**
     * Return the name of the algorithm.
     * 
     * @return the algorithm name.
     */
    public String getAlgorithmName();

    /**
     * return the cipher this object wraps.
     *
     * @return the cipher this object wraps.
     */
    public BlockCipher getUnderlyingCipher();

    /**
     * encrypt/decrypt a single byte.
     *
     * @param in the byte to be processed.
     * @param out the output buffer the processed byte goes into.
     * @param outOff the offset into the output byte array the processed data starts at.
     * @return the number of bytes written to out.
     * @exception DataLengthException if the output buffer is too small.
     */
    public int processByte(byte in, byte[] out, int outOff)
        throws DataLengthException;

    /**
     * process a block of bytes from in putting the result into out.
     *
     * @param in the input byte array.
     * @param inOff the offset into the in array where the data to be processed starts.
     * @param len the number of bytes to be processed.
     * @param out the output buffer the processed bytes go into.
     * @param outOff the offset into the output byte array the processed data starts at.
     * @return the number of bytes written to out.
     * @exception DataLengthException if the output buffer is too small.
     */
    public int processBytes(byte[] in, int inOff, int len, byte[] out, int outOff)
        throws DataLengthException;

    /**
     * Finish the operation either appending or verifying the MAC at the end of the data.
     *
     * @param out space for any resulting output data.
     * @param outOff offset into out to start copying the data at.
     * @return number of bytes written into out.
     * @throws IllegalStateException if the cipher is in an inappropriate state.
     * @throws com.dotcms.enterprise.license.bouncycastle.crypto.InvalidCipherTextException if the MAC fails to match.
     */
    public int doFinal(byte[] out, int outOff)
        throws IllegalStateException, InvalidCipherTextException;

    /**
     * Return the value of the MAC associated with the last stream processed.
     *
     * @return MAC for plaintext data.
     */
    public byte[] getMac();

    /**
     * return the size of the output buffer required for a processBytes
     * an input of len bytes.
     *
     * @param len the length of the input.
     * @return the space required to accommodate a call to processBytes
     * with len bytes of input.
     */
    public int getUpdateOutputSize(int len);

    /**
     * return the size of the output buffer required for a processBytes plus a
     * doFinal with an input of len bytes.
     *
     * @param len the length of the input.
     * @return the space required to accommodate a call to processBytes and doFinal
     * with len bytes of input.
     */
    public int getOutputSize(int len);

    /**
     * Reset the cipher. After resetting the cipher is in the same state
     * as it was after the last init (if there was one).
     */
    public void reset();
}
