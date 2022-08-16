package com.dotcms.contenttype.business;

import com.dotcms.config.DotInitializer;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.ImmutableBinaryField;
import com.dotcms.contenttype.model.field.ImmutableTextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ImmutableSimpleContentType;
import com.dotcms.contenttype.model.type.SimpleContentType;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.util.Logger;
import io.vavr.control.Try;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Initialiaze content types
 * @author jsanca
 */
public class ContentTypeInitializer implements DotInitializer {

    private static final String FAVORITE_PAGE_VAR_NAME = "favoritePage";

    @Override
    public void init() {

        this.checkFavoritePage();
    }

    private void checkFavoritePage() {

        final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());

        final ContentType contentType = Try.of(()->contentTypeAPI.find(FAVORITE_PAGE_VAR_NAME)).getOrNull();
        if (null == contentType) {

            Logger.info(this, "Creating the Favorite Page Content Type...");
            final ImmutableSimpleContentType.Builder builder = ImmutableSimpleContentType.builder();
            builder.name("Favorite Page");
            builder.variable(FAVORITE_PAGE_VAR_NAME);
            final SimpleContentType simpleContentType = builder.build();

            try {

                final List<Field> newFields = new ArrayList<>();
                final ImmutableBinaryField screenshotField = ImmutableBinaryField.builder().name("Screenshot").variable("screenshot").build();
                final ImmutableTextField   titleField      = ImmutableTextField.builder().name("title").variable("title").build();
                final ImmutableTextField   urlField        = ImmutableTextField.builder().name("url").variable("url").required(true).indexed(true).build();
                final ImmutableTextField   orderField      = ImmutableTextField.builder().name("order").dataType(DataTypes.INTEGER).variable("order").build();

                newFields.add(screenshotField);
                newFields.add(titleField);
                newFields.add(urlField);
                newFields.add(orderField);
                final ContentType savedContentType = contentTypeAPI.save(simpleContentType, newFields, null);

                final Set<String> workflowIds = new HashSet<>();
                workflowIds.add(WorkflowAPI.SYSTEM_WORKFLOW_ID);

                APILocator.getWorkflowAPI().saveSchemeIdsForContentType(savedContentType, workflowIds);
            } catch (DotDataException | DotSecurityException e) {

                Logger.warnAndDebug(this.getClass(), e);
            }
        }
    }
}
