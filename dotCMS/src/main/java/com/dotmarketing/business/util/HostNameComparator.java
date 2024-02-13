package com.dotmarketing.business.util;

import java.util.Comparator;

import com.dotmarketing.beans.Host;
import com.dotmarketing.util.Logger;

public class HostNameComparator implements Comparator<Host> {

	public int compare(Host o1, Host o2) {
		if (o1 == null) {
			throw new IllegalArgumentException("Argument string cannot be null in 1");
		} else if (o1.getHostname() == null) {
			Logger.info(this,"hostCompare1 name is null: " + o1.getIdentifier());
		}

		if (o2 == null) {
			throw new IllegalArgumentException("Argument string cannot be null in 2");
		} else if (o2.getHostname() == null) {
			Logger.info(this,"hostCompare2 name is null: " + o2.getIdentifier());
		}

		String host1 = o1.getHostname().toUpperCase();
		String host2 = o2.getHostname().toUpperCase();
		return host1.compareTo(host2);
	}

}
