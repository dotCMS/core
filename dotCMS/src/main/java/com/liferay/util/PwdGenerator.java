/**
 * Copyright (c) 2000-2005 Liferay, LLC. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.liferay.util;

import org.apache.commons.lang.RandomStringUtils;

/**
 * <a href="PwdGenerator.java.html"><b><i>View Source</i></b></a>
 *
 * @author Brian Wing Shun Chan
 * @version $Revision: 1.6 $
 */
public class PwdGenerator {

    public static String KEY1 = "0123456789";
    public static String KEY2 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    public static String KEY3 = "abcdefghijklmnopqrstuvwxyz";
    public static String KEY4 = "`~!@#$%^&*()_-+={}[]\\|:;\"'<>,.?/";

    public static String getPinNumber () {
        return _getPassword( KEY1, 4 );
    }

    public static String getPassword () {
        return getPassword( 12 );
    }

    public static String getPassword ( int length ) {
        return _getPassword( KEY1 + KEY2 + KEY3 + KEY4, length );
    }

    public static String getPassword ( String key, int length ) {
        return _getPassword( key, length );
    }

    private static String _getPassword ( String key, int length ) {
        return RandomStringUtils.random( length, key );
    }

}