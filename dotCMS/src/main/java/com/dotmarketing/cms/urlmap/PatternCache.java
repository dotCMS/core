package com.dotmarketing.cms.urlmap;

import com.dotmarketing.util.UtilMethods;
import com.liferay.util.StringPool;
import java.util.List;
import org.jetbrains.annotations.NotNull;

class PatternCache implements Comparable<PatternCache> {

    private String regEx;
    private String structureInode;
    private String URLpattern;
    private List<String> fieldMatches;

    public void setRegEx(String regEx) {
        this.regEx = regEx;
    }

    public String getRegEx() {
        return regEx;
    }

    public void setStructureInode(String structureInode) {
        this.structureInode = structureInode;
    }

    public String getStructureInode() {
        return structureInode;
    }

    public void setURLpattern(String uRLpattern) {
        URLpattern = uRLpattern;
    }

    @SuppressWarnings("unused")
    public String getURLpattern() {
        return URLpattern;
    }

    public void setFieldMatches(List<String> fieldMatches) {
        this.fieldMatches = fieldMatches;
    }

    public List<String> getFieldMatches() {
        return fieldMatches;
    }


    @Override
    public int compareTo(@NotNull final PatternCache anotherPatternCache) {
        String regex1 = this.getRegEx();
        String regex2 = anotherPatternCache.getRegEx();

        if (!regex1.endsWith(StringPool.FORWARD_SLASH)) {
            regex1 += StringPool.FORWARD_SLASH;
        }

        if (!regex2.endsWith(StringPool.FORWARD_SLASH)) {
            regex2 += StringPool.FORWARD_SLASH;
        }

        return getSlashCount(regex1) - getSlashCount(regex2);
    }

    private int getSlashCount(final String string) {
        int ret = 0;
        if (UtilMethods.isSet(string)) {
            for (int i = 0; i < string.length(); i++) {
                if (string.charAt(i) == '/') {
                    ret += 1;
                }
            }
        }
        return ret;
    }
}
