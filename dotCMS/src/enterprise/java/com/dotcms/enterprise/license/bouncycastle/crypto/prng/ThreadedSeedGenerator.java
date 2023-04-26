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

package com.dotcms.enterprise.license.bouncycastle.crypto.prng;

/**
 * A thread based seed generator - one source of randomness.
 * <p>
 * Based on an idea from Marcus Lippert.
 * </p>
 */
public class ThreadedSeedGenerator
{
    private class SeedGenerator
        implements Runnable
    {
        private volatile int counter = 0;
        private volatile boolean stop = false;

        public void run()
        {
            while (!this.stop)
            {
                this.counter++;
            }

        }

        public byte[] generateSeed(
            int numbytes,
            boolean fast)
        {
            Thread t = new Thread(this);
            byte[] result = new byte[numbytes];
            this.counter = 0;
            this.stop = false;
            int last = 0;
            int end;

            t.start();
            if(fast)
            {
                end = numbytes;
            }
            else
            {
                end = numbytes * 8;
            }
            for (int i = 0; i < end; i++)
            {
                while (this.counter == last)
                {
                    try
                    {
                        Thread.sleep(1);
                    }
                    catch (InterruptedException e)
                    {
                        // ignore
                    }
                }
                last = this.counter;
                if (fast)
                {
                    result[i] = (byte) (last & 0xff);
                }
                else
                {
                    int bytepos = i/8;
                    result[bytepos] = (byte) ((result[bytepos] << 1) | (last & 1));
                }

            }
            stop = true;
            return result;
        }
    }

    /**
     * Generate seed bytes. Set fast to false for best quality.
     * <p>
     * If fast is set to true, the code should be round about 8 times faster when
     * generating a long sequence of random bytes. 20 bytes of random values using
     * the fast mode take less than half a second on a Nokia e70. If fast is set to false,
     * it takes round about 2500 ms.
     * </p>
     * @param numBytes the number of bytes to generate
     * @param fast true if fast mode should be used
     */
    public byte[] generateSeed(
        int numBytes,
        boolean fast)
    {
        SeedGenerator gen = new SeedGenerator();

        return gen.generateSeed(numBytes, fast);
    }
}
