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

/**
 * <a href="Distance.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.5 $
 *
 */
public class Distance {

	public static double calculate(double lat1, double lon1,
								   double lat2, double lon2) {

		// Convert from radians to degrees

		lat1 = (Math.PI * lat1) / 180;
		lon1 = (Math.PI * lon1) / 180;
		lat2 = (Math.PI * lat2) / 180;
		lon2 = (Math.PI * lon2) / 180;

		double miles =
			3963.4 *
			Math.acos(
				(Math.sin(lat1) * Math.sin(lat2)) +
				(Math.cos(lat1) * Math.cos(lat2) * Math.cos(lon1 - lon2)));

		return miles;
	}

	public static double kmToMiles(double km) {
		return km * 0.621;
	}

	public static double milesToKm(double miles) {
		return miles / 0.621;
	}

}