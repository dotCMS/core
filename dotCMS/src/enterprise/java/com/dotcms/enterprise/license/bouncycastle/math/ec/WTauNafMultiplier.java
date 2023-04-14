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

/**
 * Class implementing the WTNAF (Window
 * <code>&tau;</code>-adic Non-Adjacent Form) algorithm.
 */
class WTauNafMultiplier implements ECMultiplier
{
    /**
     * Multiplies a {@link com.dotcms.enterprise.license.bouncycastle.math.ec.ECPoint.F2m ECPoint.F2m}
     * by <code>k</code> using the reduced <code>&tau;</code>-adic NAF (RTNAF)
     * method.
     * @param p The ECPoint.F2m to multiply.
     * @param k The integer by which to multiply <code>k</code>.
     * @return <code>p</code> multiplied by <code>k</code>.
     */
    public ECPoint multiply(ECPoint point, BigInteger k, PreCompInfo preCompInfo)
    {
        if (!(point instanceof ECPoint.F2m))
        {
            throw new IllegalArgumentException("Only ECPoint.F2m can be " +
                    "used in WTauNafMultiplier");
        }

        ECPoint.F2m p = (ECPoint.F2m)point;

        ECCurve.F2m curve = (ECCurve.F2m) p.getCurve();
        int m = curve.getM();
        byte a = curve.getA().toBigInteger().byteValue();
        byte mu = curve.getMu();
        BigInteger[] s = curve.getSi();

        ZTauElement rho = Tnaf.partModReduction(k, m, a, s, mu, (byte)10);

        return multiplyWTnaf(p, rho, preCompInfo, a, mu);
    }

    /**
     * Multiplies a {@link com.dotcms.enterprise.license.bouncycastle.math.ec.ECPoint.F2m ECPoint.F2m}
     * by an element <code>&lambda;</code> of <code><b>Z</b>[&tau;]</code> using
     * the <code>&tau;</code>-adic NAF (TNAF) method.
     * @param p The ECPoint.F2m to multiply.
     * @param lambda The element <code>&lambda;</code> of
     * <code><b>Z</b>[&tau;]</code> of which to compute the
     * <code>[&tau;]</code>-adic NAF.
     * @return <code>p</code> multiplied by <code>&lambda;</code>.
     */
    private ECPoint.F2m multiplyWTnaf(ECPoint.F2m p, ZTauElement lambda,
            PreCompInfo preCompInfo, byte a, byte mu)
    {
        ZTauElement[] alpha;
        if (a == 0)
        {
            alpha = Tnaf.alpha0;
        }
        else
        {
            // a == 1
            alpha = Tnaf.alpha1;
        }

        BigInteger tw = Tnaf.getTw(mu, Tnaf.WIDTH);

        byte[]u = Tnaf.tauAdicWNaf(mu, lambda, Tnaf.WIDTH,
                BigInteger.valueOf(Tnaf.POW_2_WIDTH), tw, alpha);

        return multiplyFromWTnaf(p, u, preCompInfo);
    }

    /**
     * Multiplies a {@link com.dotcms.enterprise.license.bouncycastle.math.ec.ECPoint.F2m ECPoint.F2m}
     * by an element <code>&lambda;</code> of <code><b>Z</b>[&tau;]</code>
     * using the window <code>&tau;</code>-adic NAF (TNAF) method, given the
     * WTNAF of <code>&lambda;</code>.
     * @param p The ECPoint.F2m to multiply.
     * @param u The the WTNAF of <code>&lambda;</code>..
     * @return <code>&lambda; * p</code>
     */
    private static ECPoint.F2m multiplyFromWTnaf(ECPoint.F2m p, byte[] u,
            PreCompInfo preCompInfo)
    {
        ECCurve.F2m curve = (ECCurve.F2m)p.getCurve();
        byte a = curve.getA().toBigInteger().byteValue();

        ECPoint.F2m[] pu;
        if ((preCompInfo == null) || !(preCompInfo instanceof WTauNafPreCompInfo))
        {
            pu = Tnaf.getPreComp(p, a);
            p.setPreCompInfo(new WTauNafPreCompInfo(pu));
        }
        else
        {
            pu = ((WTauNafPreCompInfo)preCompInfo).getPreComp();
        }

        // q = infinity
        ECPoint.F2m q = (ECPoint.F2m) p.getCurve().getInfinity();
        for (int i = u.length - 1; i >= 0; i--)
        {
            q = Tnaf.tau(q);
            if (u[i] != 0)
            {
                if (u[i] > 0)
                {
                    q = q.addSimple(pu[u[i]]);
                }
                else
                {
                    // u[i] < 0
                    q = q.subtractSimple(pu[-u[i]]);
                }
            }
        }

        return q;
    }
}
