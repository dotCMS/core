package com.dotmarketing.business.util;

import com.dotmarketing.beans.Host;
import java.util.Comparator;

public class HostNameComparator implements Comparator<Host> {

  public int compare(Host o1, Host o2) {
    String host1 = o1.getHostname().toUpperCase();
    String host2 = o2.getHostname().toUpperCase();
    return host1.compareTo(host2);
  }
}
