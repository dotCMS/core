package com.dotmarketing.portlets.rules.parameter.comparison;

import com.dotcms.util.HttpRequestDataUtil;

/**
 * @author Geoff M. Granum
 */
public class NetmaskComparison extends Comparison<String> {

    public NetmaskComparison() {
        super("netmask");
    }

    @Override
    public boolean perform(String ipAddress, String netmask) {
        return HttpRequestDataUtil.isIpMatchingNetmask(ipAddress, netmask);
    }
}
 
