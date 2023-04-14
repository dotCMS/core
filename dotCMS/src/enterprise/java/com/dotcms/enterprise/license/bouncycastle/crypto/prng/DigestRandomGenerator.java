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

import com.dotcms.enterprise.license.bouncycastle.crypto.Digest;

/**
 * Random generation based on the digest with counter. Calling addSeedMaterial will
 * always increase the entropy of the hash.
 * <p>
 * Internal access to the digest is synchronized so a single one of these can be shared.
 * </p>
 */
public class DigestRandomGenerator
    implements RandomGenerator
{
    private static long         CYCLE_COUNT = 10;

    private long                stateCounter;
    private long                seedCounter;
    private Digest              digest;
    private byte[]              state;
    private byte[]              seed;

    // public constructors
    public DigestRandomGenerator(
        Digest digest)
    {
        this.digest = digest;

        this.seed = new byte[digest.getDigestSize()];
        this.seedCounter = 1;

        this.state = new byte[digest.getDigestSize()];
        this.stateCounter = 1;
    }

    public void addSeedMaterial(byte[] inSeed)
    {
        synchronized (this)
        {
            digestUpdate(inSeed);
            digestUpdate(seed);
            digestDoFinal(seed);
        }
    }

    public void addSeedMaterial(long rSeed)
    {
        synchronized (this)
        {
            digestAddCounter(rSeed);
            digestUpdate(seed);

            digestDoFinal(seed);
        }
    }

    public void nextBytes(byte[] bytes)
    {
        nextBytes(bytes, 0, bytes.length);
    }

    public void nextBytes(byte[] bytes, int start, int len)
    {
        synchronized (this)
        {
            int stateOff = 0;

            generateState();

            int end = start + len;
            for (int i = start; i != end; i++)
            {
                if (stateOff == state.length)
                {
                    generateState();
                    stateOff = 0;
                }
                bytes[i] = state[stateOff++];
            }
        }
    }

    private void cycleSeed()
    {
        digestUpdate(seed);
        digestAddCounter(seedCounter++);

        digestDoFinal(seed);
    }

    private void generateState()
    {
        digestAddCounter(stateCounter++);
        digestUpdate(state);
        digestUpdate(seed);

        digestDoFinal(state);

        if ((stateCounter % CYCLE_COUNT) == 0)
        {
            cycleSeed();
        }
    }

    private void digestAddCounter(long seed)
    {
        for (int i = 0; i != 8; i++)
        {
            digest.update((byte)seed);
            seed >>>= 8;
        }
    }

    private void digestUpdate(byte[] inSeed)
    {
        digest.update(inSeed, 0, inSeed.length);
    }

    private void digestDoFinal(byte[] result)
    {
        digest.doFinal(result, 0);
    }
}
