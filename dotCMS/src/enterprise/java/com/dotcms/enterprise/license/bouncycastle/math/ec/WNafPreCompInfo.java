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

/**
 * Class holding precomputation data for the WNAF (Window Non-Adjacent Form)
 * algorithm.
 */
class WNafPreCompInfo implements PreCompInfo
{
    /**
     * Array holding the precomputed <code>ECPoint</code>s used for the Window
     * NAF multiplication in <code>
     * {@link org.bouncycastle.math.ec.multiplier.WNafMultiplier.multiply()
     * WNafMultiplier.multiply()}</code>.
     */
    private ECPoint[] preComp = null;

    /**
     * Holds an <code>ECPoint</code> representing twice(this). Used for the
     * Window NAF multiplication in <code>
     * {@link org.bouncycastle.math.ec.multiplier.WNafMultiplier.multiply()
     * WNafMultiplier.multiply()}</code>.
     */
    private ECPoint twiceP = null;

    protected ECPoint[] getPreComp()
    {
        return preComp;
    }

    protected void setPreComp(ECPoint[] preComp)
    {
        this.preComp = preComp;
    }

    protected ECPoint getTwiceP()
    {
        return twiceP;
    }

    protected void setTwiceP(ECPoint twiceThis)
    {
        this.twiceP = twiceThis;
    }
}
