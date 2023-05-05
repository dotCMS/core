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

package com.dotcms.enterprise.license.bouncycastle.crypto.modes.gcm;

import com.dotcms.enterprise.license.bouncycastle.crypto.util.Pack;

public class Tables8kGCMMultiplier implements GCMMultiplier
{
    private final int[][][] M = new int[32][16][];

    public void init(byte[] H)
    {
        M[0][0] = new int[4];
        M[1][0] = new int[4];
        M[1][8] = GCMUtil.asInts(H);

        for (int j = 4; j >= 1; j >>= 1)
        {
            int[] tmp = new int[4];
            System.arraycopy(M[1][j + j], 0, tmp, 0, 4);

            GCMUtil.multiplyP(tmp);
            M[1][j] = tmp;
        }

        {
            int[] tmp = new int[4];
            System.arraycopy(M[1][1], 0, tmp, 0, 4);

            GCMUtil.multiplyP(tmp);
            M[0][8] = tmp;
        }

        for (int j = 4; j >= 1; j >>= 1)
        {
            int[] tmp = new int[4];
            System.arraycopy(M[0][j + j], 0, tmp, 0, 4);

            GCMUtil.multiplyP(tmp);
            M[0][j] = tmp;
        }

        int i = 0;
        for (;;)
        {
            for (int j = 2; j < 16; j += j)
            {
                for (int k = 1; k < j; ++k)
                {
                    int[] tmp = new int[4];
                    System.arraycopy(M[i][j], 0, tmp, 0, 4);

                    GCMUtil.xor(tmp, M[i][k]);
                    M[i][j + k] = tmp;
                }
            }

            if (++i == 32)
            {
                return;
            }

            if (i > 1)
            {
                M[i][0] = new int[4];
                for(int j = 8; j > 0; j >>= 1)
                {
                  int[] tmp = new int[4];
                  System.arraycopy(M[i - 2][j], 0, tmp, 0, 4);

                  GCMUtil.multiplyP8(tmp);
                  M[i][j] = tmp;
                }
            }
        }
    }

    public void multiplyH(byte[] x)
    {
//      assert x.Length == 16;

        int[] z = new int[4];
        for (int i = 15; i >= 0; --i)
        {
//            GCMUtil.xor(z, M[i + i][x[i] & 0x0f]);
            int[] m = M[i + i][x[i] & 0x0f];
            z[0] ^= m[0];
            z[1] ^= m[1];
            z[2] ^= m[2];
            z[3] ^= m[3];
//            GCMUtil.xor(z, M[i + i + 1][(x[i] & 0xf0) >>> 4]);
            m = M[i + i + 1][(x[i] & 0xf0) >>> 4];
            z[0] ^= m[0];
            z[1] ^= m[1];
            z[2] ^= m[2];
            z[3] ^= m[3];
        }

        Pack.intToBigEndian(z[0], x, 0);
        Pack.intToBigEndian(z[1], x, 4);
        Pack.intToBigEndian(z[2], x, 8);
        Pack.intToBigEndian(z[3], x, 12);
    }
}
