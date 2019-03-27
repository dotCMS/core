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

import java.util.Arrays;

/**
 * <a href="StateUtil.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.6 $
 *
 */
public class StateUtil {

	public static final String[] STATE_IDS = new String[] {
		"AL", "AK", "AZ", "AR", "CA", "CO", "CT", "DE", "DC", "FL", "GA", "HI",
		"ID", "IL", "IN", "IA", "KS", "KY", "LA", "ME", "MD", "MA", "MI", "MN",
		"MS", "MO", "MT", "NE", "NV", "NH", "NJ", "NM", "NY", "NC", "ND", "OH",
		"OK", "OR", "PA", "RI", "SC", "SD", "TN", "TX", "UT", "VT", "VA", "WA",
		"WV", "WI", "WY"
	};

	public static final String[] STATE_IDS_ORDERED = new String[] {
		"AK", "AL", "AR", "AZ", "CA", "CO", "CT", "DC", "DE", "FL", "GA", "HI",
		"IA", "ID", "IL", "IN", "KS", "KY", "LA", "MA", "MD", "ME", "MI", "MN",
		"MO", "MS", "MT", "NC", "ND", "NE", "NH", "NJ", "NM", "NV", "NY", "OH",
		"OK", "OR", "PA", "RI", "SC", "SD", "TN", "TX", "UT", "VA", "VT", "WA",
		"WI", "WV", "WY"
	};

	public static final State[] STATES = new State[] {
		new State("AL", "Alabama"),
		new State("AK", "Alaska"),
		new State("AZ", "Arizona"),
		new State("AR", "Arkansas"),
		new State("CA", "California"),
		new State("CO", "Colorado"),
		new State("CT", "Connecticut"),
		new State("DE", "Delaware"),
		new State("DC", "District of Columbia"),
		new State("FL", "Florida"),
		new State("GA", "Georgia"),
		new State("HI", "Hawaii"),
		new State("ID", "Idaho"),
		new State("IL", "Illinois"),
		new State("IN", "Indiana"),
		new State("IA", "Iowa"),
		new State("KS", "Kansas"),
		new State("KY", "Kentucky"),
		new State("LA", "Louisiana"),
		new State("ME", "Maine"),
		new State("MD", "Maryland"),
		new State("MA", "Massachusetts"),
		new State("MI", "Michigan"),
		new State("MN", "Minnesota"),
		new State("MS", "Mississippi"),
		new State("MO", "Missouri"),
		new State("MT", "Montana"),
		new State("NE", "Nebraska"),
		new State("NV", "Nevada"),
		new State("NH", "New Hampshire"),
		new State("NJ", "New Jersey"),
		new State("NM", "New Mexico"),
		new State("NY", "New York"),
		new State("NC", "North Carolina"),
		new State("ND", "North Dakota"),
		new State("OH", "Ohio"),
		new State("OK", "Oklahoma"),
		new State("OR", "Oregon"),
		new State("PA", "Pennsylvania"),
		new State("RI", "Rhode Island"),
		new State("SC", "South Carolina"),
		new State("SD", "South Dakota"),
		new State("TN", "Tennessee"),
		new State("TX", "Texas"),
		new State("UT", "Utah"),
		new State("VT", "Vermont"),
		new State("VA", "Virginia"),
		new State("WA", "Washington"),
		new State("WV", "West Virginia"),
		new State("WI", "Wisconsin"),
		new State("WY", "Wyoming")
	};

	public static boolean isStateId(String stateId) {
		if (Arrays.binarySearch(STATE_IDS_ORDERED, stateId) >= 0) {
			return true;
		}
		else {
			return false;
		}
	}

	public static boolean isState(String state) {
		if (Arrays.binarySearch(STATES, state) >= 0) {
			return true;
		}
		else {
			return false;
		}
	}

}