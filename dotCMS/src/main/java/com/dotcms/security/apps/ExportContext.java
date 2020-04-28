package com.dotcms.security.apps;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.UtilMethods;
import java.util.HashSet;
import java.util.Set;

public class ExportContext {

    private final Set<String> applicationKeys;

    private final boolean exportAll;

    private final char[] password;

    private ExportContext(final Set<String> applicationKeys, char[] password) {
        this.applicationKeys = applicationKeys;
        this.exportAll = false;
        this.password = password;
    }

    private ExportContext(final boolean exportAll, final char[] password) {
        this.applicationKeys = null;
        this.exportAll = exportAll;
        this.password = password;
    }

    public Set<String> getApplicationKeys() {
        return applicationKeys;
    }

    public boolean isExportAll() {
        return exportAll;
    }

    public char[] getPassword() {
        return password;
    }

    static class Builder {

        private Set<String> applicationKeys = new HashSet<>();
        private boolean exportAll;
        private char[] password;

        Builder withApp(final String appKey) {
            applicationKeys.add(appKey);
            return this;
        }

        Builder doExportAll(final boolean exportAll) {
            this.exportAll = exportAll;
            return this;
        }

        Builder withPassword(char[] password) {
            this.password = password;
            return this;
        }

        ExportContext build() throws DotDataException {
            if(UtilMethods.isNotSet(this.password)){
                throw new DotDataException("Must provide a password.");
            }
            if (exportAll) {
                return new ExportContext(true, this.password);
            }
            if(!UtilMethods.isSet(this.applicationKeys)){
                throw new DotDataException("Must provide a application keys.");
            }
            return new ExportContext(this.applicationKeys, this.password);
        }

    }


}
