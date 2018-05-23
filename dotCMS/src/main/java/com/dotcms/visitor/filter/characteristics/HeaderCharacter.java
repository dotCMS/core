package com.dotcms.visitor.filter.characteristics;

import com.dotmarketing.util.Config;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

public class HeaderCharacter extends AbstractCharacter {

    private final Set<String> WHITELISTED_HEADERS = new HashSet<>(
            Arrays.asList(
                    Config.getStringProperty("WHITELISTED_HEADERS", "").toLowerCase().split(",")));


    public HeaderCharacter(AbstractCharacter incomingCharacter) {
        super(incomingCharacter);


        for (Enumeration<String> e = request.getHeaderNames(); e.hasMoreElements();) {
            String nextHeaderName = e.nextElement().toLowerCase();
            if (WHITELISTED_HEADERS.contains(nextHeaderName)) {
                getMap().put("h." + nextHeaderName, request.getHeader(nextHeaderName));
            }
        }

    }

}
