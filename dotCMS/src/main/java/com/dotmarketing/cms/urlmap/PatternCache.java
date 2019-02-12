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

    public void setRegEx(final String regEx) {
        this.regEx = regEx;
    }

    public String getRegEx() {
        return regEx;
    }

    public void setStructureInode(final String structureInode) {
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

    public void setFieldMatches(final List<String> fieldMatches) {
        this.fieldMatches = fieldMatches;
    }

    public List<String> getFieldMatches() {
        return fieldMatches;
    }


    @Override
    public int compareTo(@NotNull final PatternCache anotherPatternCache) {
        final String regex1 = this.getRegEx();
        final String regex2 = anotherPatternCache.getRegEx();

        final StringBuffer regex1Buffer = new StringBuffer(regex1);
        final StringBuffer regex2Buffer = new StringBuffer(regex2);

        if (!regex1.endsWith(StringPool.FORWARD_SLASH)) {
            regex1Buffer.append(StringPool.FORWARD_SLASH);
        }

        if (!regex2.endsWith(StringPool.FORWARD_SLASH)) {
            regex2Buffer.append(StringPool.FORWARD_SLASH);
        }

        return getSlashCount(regex1Buffer.toString()) - getSlashCount(regex2Buffer.toString());
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
