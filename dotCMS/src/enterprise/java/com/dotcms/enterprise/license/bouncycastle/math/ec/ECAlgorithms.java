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

package com.dotcms.enterprise.license.bouncycastle.math.ec;

import java.math.BigInteger;

public class ECAlgorithms
{
    public static ECPoint sumOfTwoMultiplies(ECPoint P, BigInteger a,
        ECPoint Q, BigInteger b)
    {
        ECCurve c = P.getCurve();
        if (!c.equals(Q.getCurve()))
        {
            throw new IllegalArgumentException("P and Q must be on same curve");
        }

        // TODO Add special case back in when WTNAF is enabled
//        // Point multiplication for Koblitz curves (using WTNAF) beats Shamir's trick
//        if (c instanceof ECCurve.F2m)
//        {
//            ECCurve.F2m f2mCurve = (ECCurve.F2m) c;
//            if (f2mCurve.isKoblitz())
//            {
//                return P.multiply(a).add(Q.multiply(b));
//            }
//        }

        return implShamirsTrick(P, a, Q, b);
    }

    /*
     * "Shamir's Trick", originally due to E. G. Straus
     * (Addition chains of vectors. American Mathematical Monthly,
     * 71(7):806-808, Aug./Sept. 1964)
     * <pre>
     * Input: The points P, Q, scalar k = (km?, ... , k1, k0)
     * and scalar l = (lm?, ... , l1, l0).
     * Output: R = k * P + l * Q.
     * 1: Z <- P + Q
     * 2: R <- O
     * 3: for i from m-1 down to 0 do
     * 4:        R <- R + R        {point doubling}
     * 5:        if (ki = 1) and (li = 0) then R <- R + P end if
     * 6:        if (ki = 0) and (li = 1) then R <- R + Q end if
     * 7:        if (ki = 1) and (li = 1) then R <- R + Z end if
     * 8: end for
     * 9: return R
     * </pre>
     */
    public static ECPoint shamirsTrick(ECPoint P, BigInteger k,
        ECPoint Q, BigInteger l)
    {
        if (!P.getCurve().equals(Q.getCurve()))
        {
            throw new IllegalArgumentException("P and Q must be on same curve");
        }

        return implShamirsTrick(P, k, Q, l);
    }

    private static ECPoint implShamirsTrick(ECPoint P, BigInteger k,
        ECPoint Q, BigInteger l)
    {
        int m = Math.max(k.bitLength(), l.bitLength());
        ECPoint Z = P.add(Q);
        ECPoint R = P.getCurve().getInfinity();

        for (int i = m - 1; i >= 0; --i)
        {
            R = R.twice();

            if (k.testBit(i))
            {
                if (l.testBit(i))
                {
                    R = R.add(Z);
                }
                else
                {
                    R = R.add(P);
                }
            }
            else
            {
                if (l.testBit(i))
                {
                    R = R.add(Q);
                }
            }
        }

        return R;
    }
}
