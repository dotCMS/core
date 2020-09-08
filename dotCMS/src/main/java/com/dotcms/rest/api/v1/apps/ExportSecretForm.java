package com.dotcms.rest.api.v1.apps;

import com.dotcms.rest.api.Validated;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

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
        super();
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

    @Override
    public String toString() {
        final List<String> stringsList = appKeysBySite.entrySet().stream()
                .map(entry -> "Site " + entry.getKey() + " keys: " + String
                        .join(",", entry.getValue())).collect(
                        Collectors.toList());

        return String.format("ExportSecretForm{ all={%s} {%s}}", exportAll,
                String.join("\n", stringsList));
    }
}
