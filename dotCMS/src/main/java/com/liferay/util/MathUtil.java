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

import java.text.NumberFormat;

import com.dotmarketing.util.Logger;

/**
 * <a href="MathUtil.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.6 $
 *
 */
public class MathUtil {

	public static int factorial(int x) {
		if (x < 0) {
			return 0;
		}

		int factorial = 1;

		while (x > 1) {
			factorial = factorial * x;
			x = x - 1;
		}

		return factorial;
	}

	public static double format(double x, int max, int min) {
		NumberFormat nf = NumberFormat.getInstance();
		nf.setMaximumFractionDigits(max);
		nf.setMinimumFractionDigits(min);

		try {
			Number number = nf.parse(nf.format(x));

			x = number.doubleValue();
		}
		catch (Exception e) {
			Logger.error(MathUtil.class,e.getMessage(),e);
		}

		return x;
	}

	public static boolean isEven(int x) {
		if ((x % 2) == 0) {
			return true;
		}

		return false;
	}

	public static boolean isOdd(int x) {
		return !isEven(x);
	}

	public static int[] generatePrimes(int max) {
		if (max < 2) {
			return new int[0];
		}
		else {
			boolean[] crossedOut = new boolean[max + 1];

			for (int i = 2; i < crossedOut.length; i++) {
				crossedOut[i] = false;
			}

			int limit = (int)Math.sqrt(crossedOut.length);

			for (int i = 2; i <= limit; i++) {
				if (!crossedOut[i]) {
					for (int multiple = 2 * i; multiple < crossedOut.length;
							multiple += i) {

						crossedOut[multiple] = true;
					}
				}
			}

			int uncrossedCount = 0;

			for (int i = 2; i < crossedOut.length; i++) {
				if (!crossedOut[i]) {
					uncrossedCount++;
				}
			}

			int[] result = new int[uncrossedCount];

			for (int i = 2, j = 0; i < crossedOut.length; i++) {
				if (!crossedOut[i]) {
					result[j++] = i;
				}
			}

			return result;
		}
	}

}