package com.dotcms.contenttype.business.init;

import com.dotcms.config.DotInitializer;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.ImmutableDateField;
import com.dotcms.contenttype.model.field.ImmutableSelectField;
import com.dotcms.contenttype.model.field.ImmutableTextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ImmutableSimpleContentType;
import com.dotcms.contenttype.model.type.SimpleContentType;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.util.Logger;
import io.vavr.control.Try;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DotAnnouncementsInitializer implements DotInitializer {

    public static final String DOT_ANNOUNCEMENT = "dotAnnouncement";
    public static final String URL_REGEX = "^((http|ftp|https):\\/\\/w{3}[d]*.|(http|ftp|https):\\/\\/|w{3}[d]*.)([wd._\\-#\\(\\)\\[\\),;:]+@[wd._\\-#\\(\\)\\[\\),;:])?([a-z0-9]+.)*[a-z-0-9]+.([a-z]{2,3})?[a-z]{2,6}(:[0-9]+)?(\\/[\\/a-zA-Z0-9._\\-,%s]+)*(\\/|\\?[a-z0-9=%&.\\-,#]+)?$";
    public static final String ANNOUNCEMENT_TYPES_VALUES =
            "Announcement\n"
            + "Release\n"
            + "Comment";

    @Override
    public void init() {
        addAnnouncementsContentType();
    }

    void addAnnouncementsContentType() {
        final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());
        ContentType contentType = Try.of(()->contentTypeAPI.find(DOT_ANNOUNCEMENT)).getOrNull();
        if(null == contentType){
            Logger.info(this, String.format("Creating the %s Content Type...",DOT_ANNOUNCEMENT));
            final ImmutableSimpleContentType.Builder builder = ImmutableSimpleContentType.builder();
            builder.name("Announcement")
                    .description("DotCMS Announcements")
                    .variable(DOT_ANNOUNCEMENT)
                    .host(Host.SYSTEM_HOST)
                    .host(Folder.SYSTEM_FOLDER)
                    .icon("announcement")
                    //.system(true)
                    .fixed(true);
            final SimpleContentType simpleContentType = builder.build();
            saveAnnouncementsContentTypeFields(contentTypeAPI, simpleContentType);
        }

    }

    private void saveAnnouncementsContentTypeFields(final ContentTypeAPI contentTypeAPI, final ContentType simpleContentType) {
        try {

            final ImmutableTextField titleField     = ImmutableTextField.builder().name("title").variable("title").required(true).listed(true).indexed(true).build();
            final ImmutableSelectField typeField    = ImmutableSelectField.builder().name("type").variable("type1").required(true).values(ANNOUNCEMENT_TYPES_VALUES).indexed(true).build();
            final ImmutableTextField   urlField     = ImmutableTextField.builder().name("url").variable("url").required(true).regexCheck(URL_REGEX).required(true).indexed(true).build();
            final ImmutableDateField dateField      = ImmutableDateField.builder().name("date").variable("date").required(true).indexed(true).unique(false).build();

            final List<Field> newFields = List.of(titleField, typeField, urlField, dateField);
            final ContentType savedContentType = contentTypeAPI.save(simpleContentType, newFields, null);

            final Set<String> workflowIds = new HashSet<>();
            workflowIds.add(WorkflowAPI.SYSTEM_WORKFLOW_ID);

            APILocator.getWorkflowAPI().saveSchemeIdsForContentType(savedContentType, workflowIds);
            Logger.debug(DotFavoritePageInitializer.class, "dotAnnouncement CT Saved.");

        } catch (DotDataException | DotSecurityException e) {

            Logger.warnAndDebug(this.getClass(), e);
        }
    }


}
