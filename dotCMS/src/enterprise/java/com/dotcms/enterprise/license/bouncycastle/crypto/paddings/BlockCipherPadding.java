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

package com.dotcms.enterprise.license.bouncycastle.crypto.paddings;

import java.security.SecureRandom;

import com.dotcms.enterprise.license.bouncycastle.crypto.InvalidCipherTextException;

/**
 * Block cipher padders are expected to conform to this interface
 */
public interface BlockCipherPadding
{
    /**
     * Initialise the padder.
     *
     * @param random the source of randomness for the padding, if required.
     */
    public void init(SecureRandom random)
        throws IllegalArgumentException;

    /**
     * Return the name of the algorithm the cipher implements.
     *
     * @return the name of the algorithm the cipher implements.
     */
    public String getPaddingName();

    /**
     * add the pad bytes to the passed in block, returning the
     * number of bytes added.
     * <p>
     * Note: this assumes that the last block of plain text is always 
     * passed to it inside in. i.e. if inOff is zero, indicating the
     * entire block is to be overwritten with padding the value of in
     * should be the same as the last block of plain text. The reason
     * for this is that some modes such as "trailing bit compliment"
     * base the padding on the last byte of plain text.
     * </p>
     */
    public int addPadding(byte[] in, int inOff);

    /**
     * return the number of pad bytes present in the block.
     * @exception InvalidCipherTextException if the padding is badly formed
     * or invalid.
     */
    public int padCount(byte[] in)
        throws InvalidCipherTextException;
}
