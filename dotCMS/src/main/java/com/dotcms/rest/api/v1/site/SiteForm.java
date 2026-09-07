package com.dotcms.rest.api.v1.site;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Form to create a site
 * @author jsanca
 */
@Schema(description = "Form used to create a Site (Host) in dotCMS. 'siteName' (the hostname) is the only "
        + "required field; the new site is created unpublished and must be published separately.")
public class SiteForm {

    @Schema(description = "Identifier of the site. Ignored on creation; server-generated.")
    private final String identifier;

    @Schema(description = "Inode (version identifier) of the site. Ignored on creation; server-generated.")
    private final String inode;

    @Schema(description = "Comma- or newline-separated list of host aliases (alternate hostnames) for this site.")
    private final String aliases;

    @Schema(description = "The hostname of the site, e.g. 'www.example.com'. This is the site's primary name.",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private final String siteName;

    @Schema(description = "Identifier of the site whose tag storage this site shares. "
            + "Defaults to this site itself when omitted.")
    private final String tagStorage;

    @Schema(description = "Identifier of the image asset used as the site thumbnail.")
    private final String siteThumbnail;

    @Schema(description = "Whether the analytics dashboard runs for this site.")
    private final boolean runDashboard;

    @Schema(description = "Default meta keywords applied to pages on this site.")
    private final String keywords;

    @Schema(description = "Default meta description applied to pages on this site.")
    private final String description;

    @Schema(description = "Google Maps API key for this site.")
    private final String googleMap;

    @Schema(description = "Google Analytics tracking ID for this site.")
    private final String googleAnalytics;

    @Schema(description = "AddThis sharing-widget account ID for this site.")
    private final String addThis;

    @Schema(description = "Proxy URL used to render the site in edit mode.")
    private final String proxyUrlForEditMode;

    @Schema(description = "Embedded dashboard markup for this site.")
    private final String embeddedDashboard;

    @Schema(description = "Default language ID for this site. Defaults to the system default language when 0/omitted.")
    private final long   languageId;

    @Schema(description = "Whether this site should become the default site. The JSON property name is 'default'. "
            + "Only one site can be the default at a time.")
    private final boolean isDefault;

    @Schema(description = "Whether to force creation even when validation would otherwise warn "
            + "(e.g. a duplicate alias).")
    private final boolean forceExecution;

    @Schema(description = "Optional list of site variables (key/value pairs) to create alongside the site.")
    private final List<SimpleSiteVariableForm> variables;

    @JsonCreator
    public SiteForm(@JsonProperty("aliases") final String aliases,
            @JsonProperty("siteName") final String siteName,
            @JsonProperty("tagStorage") final String tagStorage,
            @JsonProperty("siteThumbnail") final String siteThumbnail,
            @JsonProperty("runDashboard") final boolean runDashboard,
            @JsonProperty("keywords") final String keywords,
            @JsonProperty("description") final String description,
            @JsonProperty("googleMap") final String googleMap,
            @JsonProperty("googleAnalytics") final String googleAnalytics,
            @JsonProperty("addThis") final String addThis,
            @JsonProperty("proxyUrlForEditMode") final String proxyUrlForEditMode,
            @JsonProperty("embeddedDashboard") final String embeddedDashboard,
            @JsonProperty("languageId") final long languageId,
            @JsonProperty("identifier") final String identifier,
            @JsonProperty("inode") final String inode,
            @JsonProperty("default") final boolean isDefault,
            @JsonProperty("forceExecution") final boolean forceExecution,
            @JsonProperty("variables") List<SimpleSiteVariableForm> siteVariables) {

        this.aliases = aliases;
        this.siteName = siteName;
        this.tagStorage = tagStorage;
        this.siteThumbnail = siteThumbnail;
        this.runDashboard = runDashboard;
        this.keywords = keywords;
        this.description = description;
        this.googleMap = googleMap;
        this.googleAnalytics = googleAnalytics;
        this.addThis = addThis;
        this.proxyUrlForEditMode = proxyUrlForEditMode;
        this.embeddedDashboard = embeddedDashboard;
        this.languageId = languageId;
        this.identifier = identifier;
        this.inode = inode;
        this.isDefault = isDefault;
        this.forceExecution = forceExecution;
        this.variables = siteVariables;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getInode() {
        return inode;
    }

    public long getLanguageId() {
        return languageId;
    }

    public String getAliases() {
        return aliases;
    }

    public String getSiteName() {
        return siteName;
    }

    public String getTagStorage() {
        return tagStorage;
    }

    public String getSiteThumbnail() {
        return siteThumbnail;
    }

    public boolean isRunDashboard() {
        return runDashboard;
    }

    public String  getKeywords() {
        return keywords;
    }

    public String  getDescription() {
        return description;
    }

    public String  getGoogleMap() {
        return googleMap;
    }

    public String  getGoogleAnalytics() {
        return googleAnalytics;
    }

    public String  getAddThis() {
        return addThis;
    }

    public String  getProxyUrlForEditMode() {
        return proxyUrlForEditMode;
    }

    public String getEmbeddedDashboard() {
        return embeddedDashboard;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public boolean isForceExecution() {
        return forceExecution;
    }

    public List<SimpleSiteVariableForm> getVariables() {
        return variables;
    }

    @Override
    public String toString() {
        return "SiteForm{" +
                "aliases='" + aliases + '\'' +
                ", siteName='" + siteName + '\'' +
                ", tagStorage='" + tagStorage + '\'' +
                ", siteThumbnail='" + siteThumbnail + '\'' +
                ", runDashboard=" + runDashboard +
                ", keywords=" + keywords +
                ", description=" + description +
                ", googleMap=" + googleMap +
                ", googleAnalytics=" + googleAnalytics +
                ", addThis=" + addThis +
                ", proxyUrlForEditMode=" + proxyUrlForEditMode +
                ", default='" + isDefault + '\'' +
                ", embeddedDashboard='" + embeddedDashboard + '\'' +
                '}';
    }
}
