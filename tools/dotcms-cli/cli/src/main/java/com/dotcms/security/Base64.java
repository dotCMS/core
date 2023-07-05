package com.dotcms.security;

/**
 * Copyright (c) 2000-2005 Liferay, LLC. All rights reserved.
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

/**
 * <a href="Base64.java.html"><b><i>View Source</i></b></a>
 *
 * @author Brian Wing Shun Chan
 * @version $Revision: 1.6 $
 */
public class Base64 {

    protected static char getChar(int sixbit) {
        if (sixbit >= 0 && sixbit <= 25) {
            return (char) (65 + sixbit);
        }

        if (sixbit >= 26 && sixbit <= 51) {
            return (char) (97 + (sixbit - 26));
        }

        if (sixbit >= 52 && sixbit <= 61) {
            return (char) (48 + (sixbit - 52));
        }

        if (sixbit == 62) {
            return '+';
        }

        return sixbit != 63 ? '?' : '/';
    }

    public static String encode(byte raw[]) {
        StringBuffer encoded = new StringBuffer();

        for (int i = 0; i < raw.length; i += 3) {
            encoded.append(encodeBlock(raw, i));
        }

        return encoded.toString();
    }

    protected static char[] encodeBlock(byte raw[], int offset) {
        int block = 0;
        int slack = raw.length - offset - 1;
        int end = slack < 2 ? slack : 2;

        for (int i = 0; i <= end; i++) {
            byte b = raw[offset + i];

            int neuter = b >= 0 ? ((int) (b)) : b + 256;
            block += neuter << 8 * (2 - i);
        }

        char base64[] = new char[4];

        for (int i = 0; i < 4; i++) {
            int sixbit = block >>> 6 * (3 - i) & 0x3f;
            base64[i] = getChar(sixbit);
        }

        if (slack < 1) {
            base64[2] = '=';
        }

        if (slack < 2) {
            base64[3] = '=';
        }

        return base64;
    }

}