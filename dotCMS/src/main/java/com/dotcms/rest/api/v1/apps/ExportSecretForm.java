package com.dotcms.rest.api.v1.apps;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import com.dotcms.rest.api.Validated;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Set;
import java.util.Map;

public class ExportSecretForm extends Validated {

    @NotNull
    @Min(14)
    @Max(32)
    private final String password;

    @NotNull
    private final Map<String, Set<String>> appKeysBySite;

    @NotNull
    private final boolean exportAll;

    @JsonCreator
    public ExportSecretForm(@JsonProperty("password") final String password, @JsonProperty("exportAll") final boolean exportAll, @JsonProperty("appKeys") final Map<String, Set<String>> appKeysBySite) {
        this.password = password;
        this.exportAll = exportAll;
        this.appKeysBySite = appKeysBySite;
    }

    public String getPassword() {
        return password;
    }

    public boolean isExportAll() {
        return exportAll;
    }

    public Map<String, Set<String>> getAppKeysBySite() {
        return appKeysBySite;
    }
}
