package com.dotcms.security.apps;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Serialization Convenience Wrapper
 * Allows to collect and encapsulate a set of AppSecrets
 */
public class AppsSecretsImportExport implements Serializable {

    static final long serialVersionUID = 1L;

    private final Map<String, List<AppSecrets>> secrets;

    /**
     * Public constructor
     * @param secrets
     */
    public AppsSecretsImportExport(
            Map<String, List<AppSecrets>> secrets) {
        this.secrets = secrets;
    }

    /**
     * secrets accessor
     * @return
     */
    public Map<String, List<AppSecrets>> getSecrets() {
        return secrets;
    }

    @Override
    public String toString() {
        final List<String> stringsList = secrets.entrySet().stream()
                .map(entry -> "{"+entry.getKey() + " with : " + entry.getValue().size()+" secrets. }")
                .collect(Collectors.toList());
        return String.format("AppsSecretsImportExport{`%s`}", stringsList);
    }
}
