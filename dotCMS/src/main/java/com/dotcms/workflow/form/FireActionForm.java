package com.dotcms.workflow.form;

import com.dotmarketing.business.PermissionAPI;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.dotcms.rest.api.Validated;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

/**
 * Form to fire an action in the workflow
 * @author jsanca
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(builder = FireActionForm.Builder.class)
@Schema(description = "Form used to fire a workflow action. The content being acted on is supplied in the "
        + "'contentlet' map; the remaining fields are workflow/publishing options applied by the action.")
public class FireActionForm extends Validated {

    @Schema(description = "Optional comment recorded on the workflow task history.")
    private final String comments;

    @Schema(description = "User or role ID to assign the task to (used by actions that reassign).")
    private final String assign;

    @Schema(description = "Publish date for the 'Publish' step, in the content type's configured date format.")
    private final String publishDate;

    @Schema(description = "Publish time for the 'Publish' step, in the content type's configured time format.")
    private final String publishTime;

    @Schema(description = "Expiration date, in the content type's configured date format.")
    private final String expireDate;

    @Schema(description = "Expiration time, in the content type's configured time format.")
    private final String expireTime;

    @Schema(description = "Set to 'true' to mark the content as never expiring.")
    private final String neverExpire;

    @Schema(description = "Push-publishing target environment(s) for a 'Push Publish' action.")
    private final String whereToSend;

    @Schema(description = "Push-publishing filter key for a 'Push Publish' action.")
    private final String filterKey;

    @Schema(description = "Free-text label describing the intent of the action (audit/UI hint).")
    private final String iWantTo;

    @Schema(description = "Lucene query selecting the content to act on, as an alternative to 'contentlet'.")
    private final String query;

    @Schema(description = "Target folder path for a 'Move' action.")
    private final String pathToMove;

    @Schema(description = "Timezone ID (e.g. 'America/New_York') used to interpret the publish/expire date-times.")
    private final String timezoneId;

    @Schema(description = "Per-content individual permissions to apply, keyed by permission type "
            + "(READ, WRITE, PUBLISH, etc.) to a list of user/role IDs.")
    private final Map<PermissionAPI.Type, List<String>> individualPermissions;

    @JsonProperty("contentlet")
    @Schema(type = "object", description = "The contentlet to create or edit, as a flat map of field-variable "
            + "names to values. Polymorphic: the allowed fields depend on the content type.\n\n"
            + "**System fields (all content):**\n"
            + "- `contentType` *(string)* — the content type's variable name (e.g. 'webPageContent'). Required when creating.\n"
            + "- `languageId` *(number)* — language ID; defaults to the system default language when omitted.\n"
            + "- `contentHost` *(string)* — host (site) **identifier** the content belongs to, **or** `hostFolder` "
            + "*(string)* — a folder **identifier**. Supply one of these.\n"
            + "- `inode` / `identifier` *(string)* — include the existing identifier (and optionally inode) to edit "
            + "existing content; omit both to create new.\n\n"
            + "⚠️ Do **not** set `host` — use `contentHost` (host id) or `hostFolder` (folder id) instead.\n\n"
            + "**Pages (`contentType: 'htmlpageasset'`)** additionally use:\n"
            + "- `title` *(string)* — the page title.\n"
            + "- `url` *(string)* — the page name (the last URL segment) within `hostFolder`.\n"
            + "- `template` *(string)* — identifier of the template to render the page.\n"
            + "- `cachettl` *(number)* — page cache time-to-live in seconds.\n"
            + "- `sortOrder` *(number)* — sort order within the folder.\n\n"
            + "Note: there is no dedicated page-create endpoint — pages are created by firing an action with an "
            + "`htmlpageasset` contentlet.",
            example = "{\"contentType\":\"htmlpageasset\",\"languageId\":1,"
                    + "\"hostFolder\":\"48190c8c-42c4-46af-8d1a-0cd5db894797\","
                    + "\"title\":\"My Page\",\"url\":\"my-page\","
                    + "\"template\":\"8e63a9c0-...\",\"cachettl\":15,\"sortOrder\":0}")
    private final Map<String, Object> contentletFormData;

    public String getComments() {
        return comments;
    }

    public String getAssign() {
        return assign;
    }

    public String getPublishDate() {
        return publishDate;
    }

    public String getPublishTime() {
        return publishTime;
    }

    public String getExpireDate() {
        return expireDate;
    }

    public String getExpireTime() {
        return expireTime;
    }

    public String getNeverExpire() {
        return neverExpire;
    }

    public String getWhereToSend() {
        return whereToSend;
    }

    public String getFilterKey() {
        return filterKey;
    }

    public String getIWantTo() {
        return iWantTo;
    }

    public Map<String, Object> getContentletFormData() {
        return contentletFormData;
    }

    public String getQuery() {
        return query;
    }

    public String getPathToMove() {
        return pathToMove;
    }

    public String getTimezoneId() {
        return timezoneId;
    }

    public Map<PermissionAPI.Type, List<String>> getIndividualPermissions() {
        return individualPermissions;
    }

    public FireActionForm(final Builder builder) {

        this.comments    = builder.comments;
        this.assign      = builder.assign;
        this.publishDate = builder.publishDate;
        this.publishTime = builder.publishTime;
        this.expireDate  = builder.expireDate;
        this.expireTime  = builder.expireTime;
        this.neverExpire = builder.neverExpire;
        this.whereToSend = builder.whereToSend;
        this.filterKey   = builder.filterKey;
        this.iWantTo     = builder.iWantTo;
        this.query       = builder.query;
        this.pathToMove  = builder.pathToMove;
        this.timezoneId  = builder.timezoneId;
        this.contentletFormData    = builder.contentlet;
        this.individualPermissions = builder.individualPermissions;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Builder {

        @JsonProperty()
        private String comments;
        @JsonProperty()
        private String assign;
        @JsonProperty()
        private String publishDate;
        @JsonProperty()
        private String publishTime;
        @JsonProperty()
        private String expireDate;
        @JsonProperty()
        private String expireTime;
        @JsonProperty()
        private String neverExpire;
        @JsonProperty()
        private String whereToSend;
        @JsonProperty()
        private String filterKey;
        @JsonProperty()
        private String iWantTo;
        @JsonProperty()
        private String query;
        @JsonProperty()
        private String pathToMove;
        @JsonProperty()
        private String timezoneId;
        @JsonProperty("contentlet")
        private Map<String, Object> contentlet;
        @JsonProperty("individualPermissions")
        private Map<PermissionAPI.Type, List<String>> individualPermissions;

        public Builder individualPermissions(final Map<PermissionAPI.Type, List<String>> individualPermissions) {
            this.individualPermissions = individualPermissions;
            return this;
        }

        public Builder pathToMove(final String pathToMove) {
            this.pathToMove = pathToMove;
            return this;
        }

        public Builder comments(final String comments) {
            this.comments = comments;
            return this;
        }

        public Builder query(final String query) {
            this.query = query;
            return this;
        }

        public Builder assign(final String assign) {
            this.assign = assign;
            return this;
        }

        public Builder publishDate(final String publishDate) {
            this.publishDate = publishDate;
            return this;
        }

        public Builder publishTime(final String publishTime) {
            this.publishTime = publishTime;
            return this;
        }

        public Builder expireDate(final String expireDate) {
            this.expireDate = expireDate;
            return this;
        }

        public Builder expireTime(final String expireTime) {
            this.expireTime = expireTime;
            return this;
        }

        public Builder neverExpire(final String neverExpire) {
            this.neverExpire = neverExpire;
            return this;
        }

        public Builder whereToSend(final String whereToSend) {
            this.whereToSend = whereToSend;
            return this;
        }

        public Builder filterKey(final String filterKey) {
            this.filterKey = filterKey;
            return this;
        }

        public Builder iWantTo(final String iWantTo) {
            this.iWantTo = iWantTo;
            return this;
        }

        public Builder timezoneId(final String timezoneId){
            this.timezoneId = timezoneId;
            return this;
        }

        @JsonProperty("contentlet")
        public Builder contentlet(final  Map<String, Object> contentletFormData) {
            this.contentlet = contentletFormData;
            return this;
        }

        public FireActionForm build() {
            return new FireActionForm(this);
        }
    }
}
