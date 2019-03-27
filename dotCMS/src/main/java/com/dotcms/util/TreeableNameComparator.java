package com.dotcms.util;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.Treeable;

import java.util.Comparator;

public class TreeableNameComparator implements Comparator<Treeable> {

	public int compare(Treeable o1, Treeable o2) {
		String t1 = o1.getName().toUpperCase();
		String t2 = o2.getName().toUpperCase();
		return t1.compareTo(t2);
	}

}
