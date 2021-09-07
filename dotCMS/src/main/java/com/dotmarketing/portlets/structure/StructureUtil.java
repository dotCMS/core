package com.dotmarketing.portlets.structure;

import com.dotmarketing.util.UtilMethods;

public class StructureUtil {

	public static String generateRegExForURLMap(String urlMapString){
		StringBuilder pattern = new StringBuilder();

		if (urlMapString != null) {
            String[] urlFrags = urlMapString.split("/");
            for (String frag : urlFrags) {
                if (UtilMethods.isSet(frag)) {
                    pattern.append("/");
                    if (!frag.startsWith("{")) {
                        pattern.append(frag);
                    } else {
                        pattern.append("(.+)");
                    }
                }
            }
            if (UtilMethods.isSet(pattern.toString())) {
                pattern.append("/");
            }
        }
		return pattern.toString();
	}

}
