package com.dotcms.security.apps;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class AppSecretsImportExport implements Serializable {

    static final long serialVersionUID = 1L;

    private Map<String, List<AppSecrets>> secrets;

    public AppSecretsImportExport(
            Map<String, List<AppSecrets>> secrets) {
        this.secrets = secrets;
    }

    public Map<String, List<AppSecrets>> getSecrets() {
        return secrets;
    }
}
